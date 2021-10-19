package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.event.CaseEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@MessageEndpoint
public class CaseEventReceiverImpl implements CaseEventReceiver {
  @Autowired private RespondentDataRepository respondentDataRepo;

  /**
   * Message end point for events from Response Management.
   *
   * @param caseEvent CaseEvent message from Response Management
   * @throws CTPException something went wrong
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent caseEvent) throws CTPException {

    CaseUpdate caseUpdate = caseEvent.getPayload().getCaseUpdate();
    String caseMessageId = caseEvent.getHeader().getMessageId().toString();

    log.info(
        "Entering acceptCaseEvent",
        kv("messageId", caseMessageId),
        kv("caseId", caseUpdate.getCaseId()));

    Optional<SurveyUpdate> surveyUpdateOpt =
        respondentDataRepo.readSurvey(caseUpdate.getSurveyId());
    if (surveyUpdateOpt.isPresent()) {
      SurveyUpdate surveyUpdate = surveyUpdateOpt.get();
      try {
        if (!surveyUpdate.getSampleDefinitionUrl().endsWith("social.json")) {
          log.warn(
              "Survey is not a social survey - discarding case",
              kv("messageId", caseMessageId),
              kv("caseId", caseUpdate.getCaseId()));
        } else {
          Optional<CollectionExercise> collectionExercise =
              respondentDataRepo.readCollectionExercise(caseUpdate.getCollectionExerciseId());
          if (collectionExercise.isEmpty()) {
            // TODO - should we NAK the event/throw exception if we do not recognize the collex and
            // allow the exception manager quarantine the event or allow to go to DLQ?
            log.warn(
                "Case CollectionExercise unknown - discarding Case",
                kv("messageId", caseMessageId),
                kv("caseId", caseUpdate.getCaseId()));
          } else {
            respondentDataRepo.writeCaseUpdate(caseUpdate);
          }
        }
      } catch (CTPException ctpEx) {
        log.error("Case Event processing failed", kv("messageId", caseMessageId), ctpEx);
        throw new CTPException(ctpEx.getFault());
      }
    } else {
      // TODO - should we NAK the event/throw exception if we do not recognize the survey and allow
      // the exception manager quarantine the event or allow to go to DLQ?
      log.warn(
          "Case Survey unknown - discarding Case",
          kv("messageId", caseMessageId),
          kv("caseId", caseUpdate.getCaseId()));
    }
  }
}
