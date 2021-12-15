package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;

@ExtendWith(MockitoExtension.class)
public class EventFilterUnitTest {

  private static final Set<String> ACCEPTED_SURVEYS = Set.of("social", "test");
  private static final String SURVEY_ID = "c45de4dc-3c3b-11e9-b210-d663bd873d93";
  private static final String CASE_ID = "c45de4dc-3c3b-11e9-b210-d663bd873d93";
  private static final String COLLEX_ID = "c45de4dc-3c3b-11e9-b210-d663bd873d93";
  private static final String MESSAGE_ID = "c45de4dc-3c3b-11e9-b210-d663bd873d93";
  @Mock AppConfig appConfig;
  @Mock SurveyRepository mockRespondentSurveyRepo;
  @Mock CollectionExerciseRepository mockRespondentCollExRepo;
  @InjectMocks EventFilter eventFilter;

  @Test
  public void test_validSurveyType() throws Exception {
    when(appConfig.getSurveys()).thenReturn(ACCEPTED_SURVEYS);
    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId(SURVEY_ID);
    surveyUpdate.setSampleDefinitionUrl("test/social.json");
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setCollectionExerciseId(COLLEX_ID);
    when(mockRespondentCollExRepo.readCollectionExercise(any()))
        .thenReturn(Optional.of(collectionExercise));
    when(mockRespondentSurveyRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    assertTrue(eventFilter.isValidEvent(SURVEY_ID, COLLEX_ID, CASE_ID, MESSAGE_ID));
  }

  @Test
  public void test_InvalidSurveyType() throws Exception {
    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId(SURVEY_ID);
    surveyUpdate.setSampleDefinitionUrl("test/socialnot.json");
    when(mockRespondentSurveyRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    assertFalse(eventFilter.isValidEvent(SURVEY_ID, COLLEX_ID, CASE_ID, MESSAGE_ID));
  }

  @Test
  public void test_acceptCaseEvent_missingCollectionExercise() throws Exception {
    when(appConfig.getSurveys()).thenReturn(ACCEPTED_SURVEYS);
    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId(SURVEY_ID);
    surveyUpdate.setSampleDefinitionUrl("test/social.json");
    when(mockRespondentCollExRepo.readCollectionExercise(any())).thenReturn(Optional.empty());
    when(mockRespondentSurveyRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    assertFalse(eventFilter.isValidEvent(SURVEY_ID, COLLEX_ID, CASE_ID, MESSAGE_ID));
  }

  @Test
  public void test_acceptCaseEvent_missingSurvey() throws Exception {
    when(mockRespondentSurveyRepo.readSurvey(any())).thenReturn(Optional.empty());
    assertFalse(eventFilter.isValidEvent(SURVEY_ID, COLLEX_ID, CASE_ID, MESSAGE_ID));
  }
}
