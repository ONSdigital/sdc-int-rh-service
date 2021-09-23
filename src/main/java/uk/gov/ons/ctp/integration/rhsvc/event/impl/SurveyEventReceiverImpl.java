package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.rhsvc.event.SurveyEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/** Service implementation responsible for receipt of Survey Events. */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@MessageEndpoint
public class SurveyEventReceiverImpl implements SurveyEventReceiver {
  @Autowired private RespondentDataRepository respondentDataRepo;

  /**
   * Message end point for events from Response Management.
   *
   * @param surveyUpdateEvent SurveyUpdateEvent message from Response Management
   * @throws CTPException something went wrong
   */
  @ServiceActivator(inputChannel = "acceptSurveyUpdateEvent")
  public void acceptSurveyUpdateEvent(SurveyUpdateEvent surveyUpdateEvent) throws CTPException {

    SurveyUpdate surveyUpdate = surveyUpdateEvent.getPayload().getSurveyUpdate();
    System.out.println(surveyUpdate);
    String surveyTransactionId = surveyUpdateEvent.getEvent().getTransactionId();

    log.info(
        "Entering acceptSurveyUpdateEvent",
        kv("transactionId", surveyTransactionId),
        kv("surveyId", surveyUpdate.getSurveyId()));

    try {
      respondentDataRepo.writeSurvey(surveyUpdate);
    } catch (CTPException ctpEx) {
      log.error(
          "Survey Event processing failed", kv("surveyTransactionId", surveyTransactionId), ctpEx);
      throw new CTPException(ctpEx.getFault());
    }
  }
}
