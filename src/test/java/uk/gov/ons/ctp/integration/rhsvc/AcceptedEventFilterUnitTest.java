package uk.gov.ons.ctp.integration.rhsvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.util.AcceptedEventFilter;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class})
@ExtendWith(MockitoExtension.class)
public class AcceptedEventFilterUnitTest {

  @Autowired private AppConfig appConfig;

  @Mock RespondentDataRepository mockRespondentDataRepo;
  @InjectMocks AcceptedEventFilter acceptedEventFilter;

  @Test
  public void test_acceptCaseEvent_success() throws Exception {
    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    surveyUpdate.setSampleDefinitionUrl("test/social.json");
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setCollectionExerciseId("d45de4dc-3c3b-11e9-b210-d663bd873d93");
    when(mockRespondentDataRepo.readCollectionExercise(any()))
        .thenReturn(Optional.of(collectionExercise));
    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    assertTrue(acceptedEventFilter.filterAcceptedEvents(surveyUpdate.getSurveyId(), collectionExercise.getCollectionExerciseId(),"b45de4dc-3c3b-11e9-b210-d663bd873d93","c45de4dc-3c3b-11e9-b210-d663bd873d93"));
  }

//  @Test
//  public void test_acceptCaseEvent_reject_SIS() throws Exception {
//    Header header = new Header();
//    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
//    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
//    caseEvent.setHeader(header);
//    SurveyUpdate surveyUpdate = new SurveyUpdate();
//    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
//    surveyUpdate.setSampleDefinitionUrl("test/socialnot.json");
//    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
//    target.acceptCaseEvent(caseEvent);
//    verify(mockRespondentDataRepo, times(0))
//        .writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
//  }
//
//  @Test
//  public void test_acceptCaseEvent_missingCollectionExcersise() throws Exception {
//    Header header = new Header();
//    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
//    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
//    SurveyUpdate surveyUpdate = new SurveyUpdate();
//    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
//    surveyUpdate.setSampleDefinitionUrl("test/asteroids.json");
//    when(mockRespondentDataRepo.readCollectionExercise(any())).thenReturn(Optional.empty());
//    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
//    caseEvent.setHeader(header);
//    target.acceptCaseEvent(caseEvent);
//    verify(mockRespondentDataRepo, times(0))
//        .writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
//  }
//
//  @Test
//  public void test_acceptCaseEvent_invalidCase() throws Exception {
//    Header header = new Header();
//    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
//    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
//    caseEvent.setHeader(header);
//    SurveyUpdate surveyUpdate = new SurveyUpdate();
//    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
//    surveyUpdate.setSampleDefinitionUrl("test/social.json");
//    CollectionExercise collectionExercise = new CollectionExercise();
//    collectionExercise.setCollectionExerciseId(
//        caseEvent.getPayload().getCaseUpdate().getCollectionExerciseId());
//    when(mockRespondentDataRepo.readCollectionExercise(any()))
//        .thenReturn(Optional.of(collectionExercise));
//    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
//    Mockito.doThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND))
//        .when(mockRespondentDataRepo)
//        .writeCaseUpdate(any());
//    assertThrows(CTPException.class, () -> target.acceptCaseEvent(caseEvent));
//  }
//
//  @Test
//  public void test_acceptCaseEvent_missingSurvey() throws Exception {
//    Header header = new Header();
//    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
//    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
//    SurveyUpdate surveyUpdate = new SurveyUpdate();
//    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
//    surveyUpdate.setSampleDefinitionUrl("test/social.json");
//    CollectionExercise collectionExercise = new CollectionExercise();
//    collectionExercise.setCollectionExerciseId(
//        caseEvent.getPayload().getCaseUpdate().getCollectionExerciseId());
//    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.empty());
//    caseEvent.setHeader(header);
//    target.acceptCaseEvent(caseEvent);
//    verify(mockRespondentDataRepo, times(0)).readCollectionExercise(any());
//    verify(mockRespondentDataRepo, times(0))
//        .writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
//  }
}
