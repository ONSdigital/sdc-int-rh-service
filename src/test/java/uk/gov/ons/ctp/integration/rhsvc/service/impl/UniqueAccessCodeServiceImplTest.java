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
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;

// ** Unit tests of the Unique Access Code Service */
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

    UacUpdate uacTest = getUAC("linkedHousehold");
    CaseUpdate caseTest = getCase("household");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCaseUpdate(CASE_ID)).thenReturn(Optional.of(caseTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCaseUpdate(CASE_ID);
    verify(eventPublisher, times(1))
        .sendEvent(
            eq(TopicType.UAC_AUTHENTICATE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(uacTest.isActive(), uacDTO.isActive());
    assertEquals(CaseStatus.OK, uacDTO.getCaseStatus());
    assertEquals(UUID.fromString(uacTest.getCaseId()), uacDTO.getCaseId());
    assertEquals(
        UUID.fromString(caseTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQid(), uacDTO.getQid());
    assertEquals(caseTest.getSample().getRegion(), uacDTO.getRegion());

    assertEquals(caseTest.getSample().getAddressLine1(), uacDTO.getAddress().getAddressLine1());
    assertEquals(caseTest.getSample().getAddressLine2(), uacDTO.getAddress().getAddressLine2());
    assertEquals(caseTest.getSample().getAddressLine3(), uacDTO.getAddress().getAddressLine3());
    assertEquals(caseTest.getSample().getTownName(), uacDTO.getAddress().getTownName());
    assertEquals(caseTest.getSample().getPostcode(), uacDTO.getAddress().getPostcode());
    assertEquals(
        caseTest.getSample().getUprn(), Long.toString(uacDTO.getAddress().getUprn().getValue()));

    assertEquals(uacTest.isReceiptReceived(), uacDTO.isReceiptReceived());
    assertEquals(uacTest.isEqLaunched(), uacDTO.isEqLaunched());
    assertEquals(uacTest.getMetadata().getWave(), uacDTO.getWave());

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertEquals(uacDTO.getCaseId(), payload.getCaseId());
    assertEquals(uacDTO.getQid(), payload.getQuestionnaireId());
  }

  @Test
  public void getUACLinkedToCaseThatCannotBeFound() throws Exception {

    ArgumentCaptor<UacAuthenticateResponse> payloadCapture =
        ArgumentCaptor.forClass(UacAuthenticateResponse.class);

    UacUpdate uacTest = getUAC("linkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCaseUpdate(CASE_ID);

    verify(eventPublisher, times(1))
        .sendEvent(
            eq(TopicType.UAC_AUTHENTICATE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(uacTest.isActive(), uacDTO.isActive());
    assertEquals(CaseStatus.UNLINKED, uacDTO.getCaseStatus());
    assertNull(uacDTO.getCaseId());
    assertNull(uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQid(), uacDTO.getQid());
    assertNull(uacDTO.getRegion());

    assertNull(uacDTO.getAddress());

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertNull(payload.getCaseId());
    assertEquals(uacDTO.getQid(), payload.getQuestionnaireId());

    assertEquals(uacTest.isReceiptReceived(), uacDTO.isReceiptReceived());
    assertEquals(uacTest.isEqLaunched(), uacDTO.isEqLaunched());
    assertEquals(uacTest.getMetadata().getWave(), uacDTO.getWave());
  }

  @Test
  public void getUACNotLInkedToCase() throws Exception {

    ArgumentCaptor<UacAuthenticateResponse> payloadCapture =
        ArgumentCaptor.forClass(UacAuthenticateResponse.class);

    UacUpdate uacTest = getUAC("unlinkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCaseUpdate(CASE_ID);
    verify(eventPublisher, times(1))
        .sendEvent(
            eq(TopicType.UAC_AUTHENTICATE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(uacTest.isActive(), uacDTO.isActive());
    assertEquals(CaseStatus.UNLINKED, uacDTO.getCaseStatus());
    assertNull(uacDTO.getCaseId());
    assertNull(uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQid(), uacDTO.getQid());
    assertNull(uacDTO.getRegion());

    assertNull(uacDTO.getAddress());

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertEquals(uacDTO.getCaseId(), payload.getCaseId());
    assertEquals(uacDTO.getQid(), payload.getQuestionnaireId());

    assertEquals(uacTest.isReceiptReceived(), uacDTO.isReceiptReceived());
    assertEquals(uacTest.isEqLaunched(), uacDTO.isEqLaunched());
    assertEquals(uacTest.getMetadata().getWave(), uacDTO.getWave());
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
    verify(dataRepo, times(0)).readCaseUpdate(CASE_ID);
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any(EventPayload.class));

    assertTrue(exceptionThrown);
  }

  private UacUpdate getUAC(String qualifier) {
    return FixtureHelper.loadClassFixtures(UacUpdate[].class, qualifier).get(0);
  }

  private CaseUpdate getCase(String qualifier) {
    return FixtureHelper.loadClassFixtures(CaseUpdate[].class, qualifier).get(0);
  }
}
