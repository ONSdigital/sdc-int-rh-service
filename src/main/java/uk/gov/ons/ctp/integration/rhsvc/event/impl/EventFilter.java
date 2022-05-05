package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;

@Slf4j
@Component
public class EventFilter {

  private AppConfig appConfig;

  private SurveyRepository respondentSurveyRepo;
  private CollectionExerciseRepository respondentCollExRepo;

  public EventFilter(
      AppConfig appConfig,
      SurveyRepository respondentSurveyRepo,
      CollectionExerciseRepository respondentCollExRepo) {
    this.appConfig = appConfig;
    this.respondentSurveyRepo = respondentSurveyRepo;
    this.respondentCollExRepo = respondentCollExRepo;
  }

  public boolean isValidEvent(String surveyId, String collexId, String caseId, String messageId)
      throws CTPException {

    Optional<SurveyUpdate> surveyUpdateOpt = respondentSurveyRepo.readSurvey(surveyId);

    if (surveyUpdateOpt.isPresent()) {
      SurveyUpdate surveyUpdate = surveyUpdateOpt.get();

      log.warn("SURVEY URL: " + surveyUpdate.getSampleDefinitionUrl());
      log.warn("SURVEY TYPE: " + surveyUpdate.surveyType());

      SurveyType[] acceptedTypes = SurveyType.values();

      if (isAcceptedSurveyType(surveyUpdate.surveyType())) {
        Optional<CollectionExerciseUpdate> collectionExercise =
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

//  public static SurveyType fromSampleDefinitionUrl(String url) {
//    SurveyType[] var1 = SurveyType.values();
//    int var2 = var1.length;
//
//    for(int var3 = 0; var3 < var2; ++var3) {
//      SurveyType type = var1[var3];
//      if (url != null && url.endsWith(typ)) {
//        return type;
//      }
//    }
//
//    return null;
//  }


  private boolean isAcceptedSurveyType(SurveyType type) {

    log.warn("**************");

    if (type == null) {
      log.warn("Received Survey Type is null");
    } else {
      log.warn("RECEIVED SURVEY TYPE: " + type.name());
    }

    String[] array = appConfig.getSurveys().toArray(new String[0]);
    log.warn("ACCEPTED SURVEY TYPES: " + Arrays.toString(array));
    log.warn("**************");

    return type != null
        && appConfig.getSurveys().stream()
            .map(s -> s.toUpperCase())
            .anyMatch(s -> s.equals(type.name()));
  }
}
