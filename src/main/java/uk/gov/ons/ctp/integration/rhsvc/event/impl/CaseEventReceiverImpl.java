package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.rhsvc.event.CaseEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;

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
  @Autowired private RespondentDataRepositoryImpl respondentDataRepo;

  @Autowired private EventFilter eventFilter;

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
    try {
      if (eventFilter.isValidEvent(
          caseUpdate.getSurveyId(),
          caseUpdate.getCollectionExerciseId(),
          caseUpdate.getCaseId(),
          caseMessageId)) {
        respondentDataRepo.writeCaseUpdate(caseUpdate);
      }
    } catch (CTPException ctpEx) {
      log.error("Case Event processing failed", kv("messageId", caseMessageId), ctpEx);
      throw ctpEx;
    }
  }
}
