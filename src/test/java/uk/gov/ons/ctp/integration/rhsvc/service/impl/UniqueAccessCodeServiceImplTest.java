package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;

/** Unit tests of the Unique Access Code Service */
@ExtendWith(MockitoExtension.class)
public class UniqueAccessCodeServiceImplTest {

  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";
  private static final String CASE_ID = "bfb5cdca-3119-4d2c-a807-51ae55443b33";

  @InjectMocks private UniqueAccessCodeServiceImpl uacSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Test
  public void getUACLinkedToExistingCase() throws Exception {

    ArgumentCaptor<UacAuthenticateResponse> payloadCapture =
        ArgumentCaptor.forClass(UacAuthenticateResponse.class);

    UAC uacTest = getUAC("linkedHousehold");
    CollectionCase caseTest = getCase("household");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCollectionCase(CASE_ID)).thenReturn(Optional.of(caseTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCollectionCase(CASE_ID);
    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.UAC_AUTHENTICATE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(CaseStatus.OK, uacDTO.getCaseStatus());
    assertEquals(UUID.fromString(uacTest.getCaseId()), uacDTO.getCaseId());
    assertEquals(
        UUID.fromString(caseTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQuestionnaireId(), uacDTO.getQuestionnaireId());
    assertEquals(caseTest.getCaseType(), uacDTO.getCaseType());
    assertEquals(caseTest.getAddress().getEstabType(), uacDTO.getEstabType());
    assertEquals(caseTest.getAddress().getRegion(), uacDTO.getRegion());
    assertEquals(uacTest.getFormType(), uacDTO.getFormType());

    assertEquals(caseTest.getAddress().getAddressLine1(), uacDTO.getAddress().getAddressLine1());
    assertEquals(caseTest.getAddress().getAddressLine2(), uacDTO.getAddress().getAddressLine2());
    assertEquals(caseTest.getAddress().getAddressLine3(), uacDTO.getAddress().getAddressLine3());
    assertEquals(caseTest.getAddress().getTownName(), uacDTO.getAddress().getTownName());
    assertEquals(caseTest.getAddress().getPostcode(), uacDTO.getAddress().getPostcode());
    assertEquals(
        caseTest.getAddress().getUprn(), Long.toString(uacDTO.getAddress().getUprn().getValue()));

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertEquals(uacDTO.getCaseId(), payload.getCaseId());
    assertEquals(uacDTO.getQuestionnaireId(), payload.getQuestionnaireId());
  }

  @Test
  public void getUACLinkedToCaseThatCannotBeFound() throws Exception {

    ArgumentCaptor<UacAuthenticateResponse> payloadCapture =
        ArgumentCaptor.forClass(UacAuthenticateResponse.class);

    UAC uacTest = getUAC("linkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCollectionCase(CASE_ID);

    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.UAC_AUTHENTICATE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(CaseStatus.UNLINKED, uacDTO.getCaseStatus());
    assertNull(uacDTO.getCaseId());
    assertNull(uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQuestionnaireId(), uacDTO.getQuestionnaireId());
    assertNull(uacDTO.getCaseType());
    assertNull(uacDTO.getRegion());
    assertNull(uacDTO.getEstabType());

    assertNull(uacDTO.getAddress());

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertNull(payload.getCaseId());
    assertEquals(uacDTO.getQuestionnaireId(), payload.getQuestionnaireId());
  }

  @Test
  public void getUACNotLInkedToCase() throws Exception {

    ArgumentCaptor<UacAuthenticateResponse> payloadCapture =
        ArgumentCaptor.forClass(UacAuthenticateResponse.class);

    UAC uacTest = getUAC("unlinkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCollectionCase(CASE_ID);
    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.UAC_AUTHENTICATE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(CaseStatus.UNLINKED, uacDTO.getCaseStatus());
    assertNull(uacDTO.getCaseId());
    assertNull(uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQuestionnaireId(), uacDTO.getQuestionnaireId());
    assertNull(uacDTO.getCaseType());
    assertNull(uacDTO.getRegion());
    assertNull(uacDTO.getEstabType());

    assertNull(uacDTO.getAddress());

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertEquals(uacDTO.getCaseId(), payload.getCaseId());
    assertEquals(uacDTO.getQuestionnaireId(), payload.getQuestionnaireId());
  }

  /** Test request for claim object where UAC not found */
  @Test
  public void getUACNotFound() throws Exception {

    boolean exceptionThrown = false;
    try {
      uacSvc.getAndAuthenticateUAC(UAC_HASH);
    } catch (CTPException e) {
      exceptionThrown = true;
    }

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCollectionCase(CASE_ID);
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any());

    assertTrue(exceptionThrown);
  }

  private UAC getUAC(String qualifier) {
    return FixtureHelper.loadClassFixtures(UAC[].class, qualifier).get(0);
  }

  private CollectionCase getCase(String qualifier) {
    return FixtureHelper.loadClassFixtures(CollectionCase[].class, qualifier).get(0);
  }
}
