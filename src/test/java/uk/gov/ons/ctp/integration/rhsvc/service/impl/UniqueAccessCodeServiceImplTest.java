package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacAuthentication;
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

  @Captor private ArgumentCaptor<UacAuthentication> uacAuthenticationCaptor;

  @Test
  public void getUACLinkedToExistingCase() throws Exception {

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
            eq(TopicType.UAC_AUTHENTICATION),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            uacAuthenticationCaptor.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(uacTest.isActive(), uacDTO.isActive());
    assertEquals(uacTest.getQid(), uacDTO.getQid());
    assertEquals(uacTest.isReceiptReceived(), uacDTO.isReceiptReceived());
    assertEquals(uacTest.isEqLaunched(), uacDTO.isEqLaunched());
    assertEquals(uacTest.getMetadata().getWave(), uacDTO.getWave());

    assertEquals(UUID.fromString(surveyTest.getSurveyId()), uacDTO.getSurvey().getSurveyId());
    assertEquals(surveyTest.getName(), uacDTO.getSurvey().getName());

    assertEquals(
        UUID.fromString(collexTest.getCollectionExerciseId()),
        uacDTO.getCollectionExercise().getCollectionExerciseId());
    assertEquals(
        UUID.fromString(collexTest.getSurveyId()), uacDTO.getCollectionExercise().getSurveyId());
    assertEquals(collexTest.getName(), uacDTO.getCollectionExercise().getName());
    assertEquals(collexTest.getReference(), uacDTO.getCollectionExercise().getReference());
    assertEquals(
        convertDate(collexTest.getStartDate()), uacDTO.getCollectionExercise().getStartDate());
    assertEquals(convertDate(collexTest.getEndDate()), uacDTO.getCollectionExercise().getEndDate());
    assertEquals(
        collexTest.getMetadata().getNumberOfWaves(),
        uacDTO.getCollectionExercise().getNumberOfWaves());
    assertEquals(
        collexTest.getMetadata().getWaveLength(), uacDTO.getCollectionExercise().getWaveLength());
    assertEquals(
        collexTest.getMetadata().getCohorts(), uacDTO.getCollectionExercise().getCohorts());
    assertEquals(
        collexTest.getMetadata().getCohortSchedule(),
        uacDTO.getCollectionExercise().getCohortSchedule());

    assertEquals(UUID.fromString(uacTest.getCaseId()), uacDTO.getCollectionCase().getCaseId());
    assertEquals(
        UUID.fromString(caseTest.getCollectionExerciseId()),
        uacDTO.getCollectionExercise().getCollectionExerciseId());
    assertEquals(caseTest.getCaseRef(), uacDTO.getCollectionCase().getCaseRef());
    assertEquals(caseTest.getSample(), uacDTO.getCollectionCase().getSample());
    assertEquals(caseTest.getSampleSensitive(), uacDTO.getCollectionCase().getSampleSensitive());

    UacAuthentication payload = uacAuthenticationCaptor.getValue();
    assertEquals(uacDTO.getQid(), payload.getQid());
  }

  @Test
  public void getUACLinkedToCaseThatCannotBeFound() throws Exception {
    UacUpdate uacTest = getUAC("linkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCaseUpdate(CASE_ID)).thenReturn(Optional.empty());

    CTPException thrown =
        assertThrows(CTPException.class, () -> uacSvc.getAndAuthenticateUAC(UAC_HASH));

    assertEquals(CTPException.Fault.SYSTEM_ERROR, thrown.getFault());
    assertEquals("Case Not Found", thrown.getMessage());

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCaseUpdate(CASE_ID);
    verify(dataRepo, times(0)).readSurvey(any());
    verify(dataRepo, times(0)).readCollectionExercise(any());

    verify(eventPublisher, times(0))
        .sendEvent(any(), any(), any(), uacAuthenticationCaptor.capture());
  }

  @Test
  public void getUACLinkedToSurveyThatCannotBeFound() throws Exception {
    UacUpdate uacTest = getUAC("linkedHousehold");
    CaseUpdate caseTest = getCase("household");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCaseUpdate(CASE_ID)).thenReturn(Optional.empty());
    when(dataRepo.readCaseUpdate(CASE_ID)).thenReturn(Optional.of(caseTest));
    when(dataRepo.readSurvey(SURVEY_ID)).thenReturn(Optional.empty());

    CTPException thrown =
        assertThrows(CTPException.class, () -> uacSvc.getAndAuthenticateUAC(UAC_HASH));

    assertEquals(CTPException.Fault.SYSTEM_ERROR, thrown.getFault());
    assertEquals("Survey Not Found", thrown.getMessage());

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCaseUpdate(CASE_ID);
    verify(dataRepo, times(1)).readSurvey(SURVEY_ID);
    verify(dataRepo, times(0)).readCollectionExercise(any());

    verify(eventPublisher, times(0))
        .sendEvent(any(), any(), any(), uacAuthenticationCaptor.capture());
  }

  @Test
  public void getUACLinkedToCollectionExerciseThatCannotBeFound() throws Exception {
    UacUpdate uacTest = getUAC("linkedHousehold");
    CaseUpdate caseTest = getCase("household");
    SurveyUpdate surveyTest = getSurvey();

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCaseUpdate(CASE_ID)).thenReturn(Optional.of(caseTest));
    when(dataRepo.readSurvey(SURVEY_ID)).thenReturn(Optional.of(surveyTest));
    when(dataRepo.readCollectionExercise(COLLECTION_EXERCISE_ID)).thenReturn(Optional.empty());

    CTPException thrown =
        assertThrows(CTPException.class, () -> uacSvc.getAndAuthenticateUAC(UAC_HASH));

    assertEquals(CTPException.Fault.SYSTEM_ERROR, thrown.getFault());
    assertEquals("CollectionExercise Not Found", thrown.getMessage());

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCaseUpdate(CASE_ID);
    verify(dataRepo, times(1)).readSurvey(SURVEY_ID);
    verify(dataRepo, times(1)).readCollectionExercise(COLLECTION_EXERCISE_ID);

    verify(eventPublisher, times(0))
        .sendEvent(any(), any(), any(), uacAuthenticationCaptor.capture());
  }

  @Test
  public void getUACNotLinkedToCase() throws Exception {
    UacUpdate uacTest = getUAC("unlinkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    CTPException thrown =
        assertThrows(CTPException.class, () -> uacSvc.getAndAuthenticateUAC(UAC_HASH));

    assertEquals(CTPException.Fault.SYSTEM_ERROR, thrown.getFault());
    assertEquals("UAC has no caseId", thrown.getMessage());

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCaseUpdate(CASE_ID);
    verify(eventPublisher, times(0))
        .sendEvent(any(), any(), any(), uacAuthenticationCaptor.capture());
  }

  /** Test request for claim object where UAC not found */
  @Test
  public void getUACNotFound() throws Exception {

    CTPException thrown =
        assertThrows(CTPException.class, () -> uacSvc.getAndAuthenticateUAC(UAC_HASH));

    assertEquals(CTPException.Fault.RESOURCE_NOT_FOUND, thrown.getFault());
    assertEquals("Failed to retrieve UAC", thrown.getMessage());

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCaseUpdate(CASE_ID);
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any(EventPayload.class));
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

  private LocalDateTime convertDate(Date date) {
    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of(ZoneOffset.UTC.getId()));
  }
}
