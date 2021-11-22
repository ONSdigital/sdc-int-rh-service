package uk.gov.ons.ctp.integration.rhsvc.util;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

@Slf4j
@Component
public class AcceptedEventFilter {

  @Autowired private AppConfig appConfig;

  @Autowired private RespondentDataRepository respondentDataRepo;

  public boolean filterAcceptedEvents(
      String suvreyId, String collexId, String caseId, String messageId) throws CTPException {
    Optional<SurveyUpdate> surveyUpdateOpt = respondentDataRepo.readSurvey(suvreyId);
    if (surveyUpdateOpt.isPresent()) {
      SurveyUpdate surveyUpdate = surveyUpdateOpt.get();
      try {
        if (isAcceptedSurveyType(surveyUpdate.getSampleDefinitionUrl())) {
          Optional<CollectionExercise> collectionExercise =
              respondentDataRepo.readCollectionExercise(collexId);
          if (collectionExercise.isPresent()) {
            return true;
          } else {
            // TODO - should we NAK the event/throw exception if we do not recognize the collex and
            // allow the exception manager quarantine the event or allow to go to DLQ?
            log.warn(
                "CollectionExercise unknown - discarding message",
                kv("messageId", messageId),
                kv("caseId", caseId));
          }
        } else {
          log.warn(
              "Survey is not a social survey - discarding message",
              kv("messageId", messageId),
              kv("caseId", caseId));
        }
      } catch (CTPException ctpEx) {
        log.error("Event processing failed", kv("messageId", messageId), ctpEx);
        throw ctpEx;
      }
    } else {
      // TODO - should we NAK the event/throw exception if we do not recognize the survey and allow
      // the exception manager quarantine the event or allow to go to DLQ?
      log.warn(
          "Survey unknown - discarding message", kv("messageId", messageId), kv("caseId", caseId));
    }
    return false;
  }

  private boolean isAcceptedSurveyType(String sampleDefinitionUrl) {
    Set<String> surveys = appConfig.getSurveyConfig().getSurveys();
    for (String survey : surveys) {
      if (sampleDefinitionUrl.endsWith(survey + ".json")) {
        return true;
      }
    }
    return false;
  }
}
