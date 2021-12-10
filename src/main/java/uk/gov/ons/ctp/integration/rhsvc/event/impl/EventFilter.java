package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentCollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentSurveyRepository;

@Slf4j
@Component
public class EventFilter {

  private AppConfig appConfig;

  private RespondentSurveyRepository respondentSurveyRepo;
  private RespondentCollectionExerciseRepository respondentCollExRepo;

  public EventFilter(
      AppConfig appConfig,
      RespondentSurveyRepository respondentSurveyRepo,
      RespondentCollectionExerciseRepository respondentCollExRepo) {
    this.appConfig = appConfig;
    this.respondentSurveyRepo = respondentSurveyRepo;
    this.respondentCollExRepo = respondentCollExRepo;
  }

  public boolean isValidEvent(String surveyId, String collexId, String caseId, String messageId)
      throws CTPException {

    Optional<SurveyUpdate> surveyUpdateOpt = respondentSurveyRepo.readSurvey(surveyId);
    if (surveyUpdateOpt.isPresent()) {
      SurveyUpdate surveyUpdate = surveyUpdateOpt.get();
      if (isAcceptedSurveyType(surveyUpdate.surveyType())) {
        Optional<CollectionExercise> collectionExercise =
            respondentCollExRepo.readCollectionExercise(collexId);
        if (collectionExercise.isPresent()) {
          return true;
        } else {
          // TODO - should we NAK the event/throw exception if we do not recognize the collex and
          // allow the exception manager to quarantine the event or allow to go to DLQ?
          log.warn(
              "CollectionExercise unknown - discarding message",
              kv("messageId", messageId),
              kv("caseId", caseId));
        }
      } else {
        log.warn(
            "Survey is not an accepted survey type - discarding message",
            kv("messageId", messageId),
            kv("caseId", caseId));
      }
    } else {
      // TODO - should we NAK the event/throw exception if we do not recognize the survey and allow
      // the exception manager to quarantine the event or allow to go to DLQ?
      log.warn(
          "Survey unknown - discarding message", kv("messageId", messageId), kv("caseId", caseId));
    }
    return false;
  }

  private boolean isAcceptedSurveyType(SurveyType type) {
    return type != null && appConfig.getSurveys().contains(type.getBasename());
  }
}
