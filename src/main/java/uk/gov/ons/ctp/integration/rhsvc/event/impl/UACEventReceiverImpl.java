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
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/**
 * Service implementation responsible for receipt of UAC Events. See Spring Integration flow for
 * details of in bound queue.
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@MessageEndpoint
public class UACEventReceiverImpl {
  @Autowired private RespondentDataRepository respondentDataRepo;
  @Autowired private AppConfig appConfig;

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param uacEvent UACEvent message (either created or updated type)from Response Management
   * @throws CTPException something went wrong
   */
  @ServiceActivator(inputChannel = "acceptUACEvent")
  public void acceptUACEvent(UacEvent uacEvent) throws CTPException {

    UAC uac = uacEvent.getPayload().getUac();
    String uacMessageId = uacEvent.getHeader().getMessageId().toString();

    log.info(
        "Entering acceptUACEvent", kv("messageId", uacMessageId), kv("caseId", uac.getCaseId()));

    String qid = uac.getQuestionnaireId();
    if (isFilteredByQid(qid)) {
      log.info(
          "Filtering UAC Event because of questionnaire ID prefix",
          kv("messageId", uacMessageId),
          kv("caseId", uac.getCaseId()),
          kv("questionnaireId", qid));
      return;
    }

    try {
      respondentDataRepo.writeUAC(uac);
    } catch (CTPException ctpEx) {
      log.error("UAC Event processing failed", kv("uacMessageId", uacMessageId), ctpEx);
      throw new CTPException(ctpEx.getFault());
    }
  }

  private boolean isFilteredByQid(String qid) {
    return qid != null
        && qid.length() > 2
        && appConfig.getQueueConfig().getQidFilterPrefixes().contains(qid.substring(0, 2));
  }
}
