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
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;

// ** Unit tests of the Unique Access Code Service */
@ExtendWith(MockitoExtension.class)
public class UniqueAccessCodeServiceImplTest {

  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";
  private static final String CASE_ID = "bfb5cdca-3119-4d2c-a807-51ae55443b33";
  private static final String SURVEY_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";
  private static final String COLLECTION_EXERCISE_ID = "44d7f3bb-91c9-45d0-bb2d-90afce4fc790";

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
    SurveyUpdate surveyTest = getSurvey();
    CollectionExercise collexTest = getCollex();

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCaseUpdate(CASE_ID)).thenReturn(Optional.of(caseTest));
    when(dataRepo.readSurvey(SURVEY_ID)).thenReturn(Optional.of(surveyTest));
    when(dataRepo.readCollectionExercise(COLLECTION_EXERCISE_ID))
        .thenReturn(Optional.of(collexTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCaseUpdate(CASE_ID);
    verify(dataRepo, times(1)).readSurvey(SURVEY_ID);
    verify(dataRepo, times(1)).readCollectionExercise(COLLECTION_EXERCISE_ID);
    verify(eventPublisher, times(1))
        .sendEvent(
            eq(TopicType.UAC_AUTHENTICATE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(uacTest.isActive(), uacDTO.isActive());
    assertEquals(uacTest.getQid(), uacDTO.getQid());
    assertEquals(uacTest.isReceiptReceived(), uacDTO.isReceiptReceived());
    assertEquals(uacTest.isEqLaunched(), uacDTO.isEqLaunched());
    assertEquals(uacTest.getMetadata().getWave(), uacDTO.getWave());

    assertEquals(surveyTest.getSurveyId(), uacDTO.getSurveyLiteDTO().getSurveyId());
    assertEquals(surveyTest.getName(), uacDTO.getSurveyLiteDTO().getName());

    assertEquals(
        collexTest.getCollectionExerciseId(),
        uacDTO.getCollectionExerciseDTO().getCollectionExerciseId());
    assertEquals(collexTest.getSurveyId(), uacDTO.getCollectionExerciseDTO().getSurveyId());
    assertEquals(collexTest.getName(), uacDTO.getCollectionExerciseDTO().getName());
    assertEquals(collexTest.getReference(), uacDTO.getCollectionExerciseDTO().getReference());
    assertEquals(collexTest.getStartDate(), uacDTO.getCollectionExerciseDTO().getStartDate());
    assertEquals(collexTest.getEndDate(), uacDTO.getCollectionExerciseDTO().getEndDate());
    assertEquals(
        collexTest.getMetadata().getNumberOfWaves(),
        uacDTO.getCollectionExerciseDTO().getNumberOfWaves());
    assertEquals(
        collexTest.getMetadata().getWaveLength(),
        uacDTO.getCollectionExerciseDTO().getWaveLength());
    assertEquals(
        collexTest.getMetadata().getCohorts(), uacDTO.getCollectionExerciseDTO().getCohorts());
    assertEquals(
        collexTest.getMetadata().getCohortSchedule(),
        uacDTO.getCollectionExerciseDTO().getCohortSchedule());

    assertEquals(UUID.fromString(uacTest.getCaseId()), uacDTO.getCaseDTO().getCaseId());
    assertEquals(
        caseTest.getCollectionExerciseId(),
        uacDTO.getCollectionExerciseDTO().getCollectionExerciseId());
    assertEquals(
        Region.valueOf(caseTest.getSample().getRegion()),
        uacDTO.getCaseDTO().getAddress().getRegion());
    assertEquals(
        caseTest.getSample().getAddressLine1(), uacDTO.getCaseDTO().getAddress().getAddressLine1());
    assertEquals(
        caseTest.getSample().getAddressLine2(), uacDTO.getCaseDTO().getAddress().getAddressLine2());
    assertEquals(
        caseTest.getSample().getAddressLine3(), uacDTO.getCaseDTO().getAddress().getAddressLine3());
    assertEquals(
        caseTest.getSample().getTownName(), uacDTO.getCaseDTO().getAddress().getTownName());
    assertEquals(
        caseTest.getSample().getPostcode(), uacDTO.getCaseDTO().getAddress().getPostcode());
    assertEquals(
        caseTest.getSample().getUprn(),
        Long.toString(uacDTO.getCaseDTO().getAddress().getUprn().getValue()));

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertEquals(uacDTO.getCaseDTO().getCaseId(), payload.getCaseId());
    assertEquals(uacDTO.getQid(), payload.getQuestionnaireId());
  }

  @Test
  public void getUACLinkedToCaseThatCannotBeFound() throws Exception {

    ArgumentCaptor<UacAuthenticateResponse> payloadCapture =
        ArgumentCaptor.forClass(UacAuthenticateResponse.class);

    UacUpdate uacTest = getUAC("linkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCaseUpdate(CASE_ID)).thenReturn(Optional.empty());

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCaseUpdate(CASE_ID);
    verify(dataRepo, times(0)).readSurvey(any());
    verify(dataRepo, times(0)).readCollectionExercise(any());

    verify(eventPublisher, times(1))
        .sendEvent(
            eq(TopicType.UAC_AUTHENTICATE),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(uacTest.isActive(), uacDTO.isActive());
    assertNull(uacDTO.getCaseDTO());
    assertNull(uacDTO.getCollectionExerciseDTO());
    assertNull(uacDTO.getSurveyLiteDTO());
    assertEquals(uacTest.getQid(), uacDTO.getQid());

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertNull(payload.getCaseId());
    assertEquals(uacDTO.getQid(), payload.getQuestionnaireId());

    assertEquals(uacTest.isReceiptReceived(), uacDTO.isReceiptReceived());
    assertEquals(uacTest.isEqLaunched(), uacDTO.isEqLaunched());
    assertEquals(uacTest.getMetadata().getWave(), uacDTO.getWave());
  }

  @Test
  public void getUACNotLinkedToCase() throws Exception {

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
    assertNull(uacDTO.getCaseDTO());
    assertNull(uacDTO.getCollectionExerciseDTO());
    assertNull(uacDTO.getSurveyLiteDTO());
    assertEquals(uacTest.getQid(), uacDTO.getQid());

    UacAuthenticateResponse payload = payloadCapture.getValue();
    assertNull(payload.getCaseId());
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

  private SurveyUpdate getSurvey() {
    return FixtureHelper.loadClassFixtures(SurveyUpdate[].class).get(0);
  }

  private CollectionExercise getCollex() {
    return FixtureHelper.loadClassFixtures(CollectionExercise[].class).get(0);
  }
}
