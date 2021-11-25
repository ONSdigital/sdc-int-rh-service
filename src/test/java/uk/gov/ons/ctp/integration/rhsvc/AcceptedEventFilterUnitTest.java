package uk.gov.ons.ctp.integration.rhsvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.InboundEventIntegrationConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.SurveyConfig;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.EventReceiverConfiguration;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.MessageIT_Config;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.util.AcceptedEventFilter;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AcceptedEventFilterUnitTest {

  private static final Set<String> ACCEPTED_SURVEYS = Set.of("social", "asteroid");
  @Mock AppConfig appConfig;
  @Mock RespondentDataRepository mockRespondentDataRepo;
  @InjectMocks AcceptedEventFilter acceptedEventFilter;

  @Test
  public void test_validSurveyType() throws Exception {
    when(appConfig.getSurveys()).thenReturn(ACCEPTED_SURVEYS);
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

  @Test
  public void test_InvalidSurveyType() throws Exception {
    when(appConfig.getSurveys()).thenReturn(ACCEPTED_SURVEYS);
    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    surveyUpdate.setSampleDefinitionUrl("test/socialnot.json");
    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    assertFalse(acceptedEventFilter.filterAcceptedEvents(surveyUpdate.getSurveyId(), "d45de4dc-3c3b-11e9-b210-d663bd873d93","b45de4dc-3c3b-11e9-b210-d663bd873d93","c45de4dc-3c3b-11e9-b210-d663bd873d93"));
  }

  @Test
  public void test_acceptCaseEvent_missingCollectionExercise() throws Exception {
    when(appConfig.getSurveys()).thenReturn(ACCEPTED_SURVEYS);
    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    surveyUpdate.setSampleDefinitionUrl("test/asteroid.json");
    when(mockRespondentDataRepo.readCollectionExercise(any())).thenReturn(Optional.empty());
    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    assertFalse(acceptedEventFilter.filterAcceptedEvents(surveyUpdate.getSurveyId(), "d45de4dc-3c3b-11e9-b210-d663bd873d93","b45de4dc-3c3b-11e9-b210-d663bd873d93","c45de4dc-3c3b-11e9-b210-d663bd873d93"));
  }

  @Test
  public void test_acceptCaseEvent_missingSurvey() throws Exception {
    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.empty());
    assertFalse(acceptedEventFilter.filterAcceptedEvents("c45de4dc-3c3b-11e9-b210-d663bd873d93", "d45de4dc-3c3b-11e9-b210-d663bd873d93","b45de4dc-3c3b-11e9-b210-d663bd873d93","c45de4dc-3c3b-11e9-b210-d663bd873d93"));
  }

}
