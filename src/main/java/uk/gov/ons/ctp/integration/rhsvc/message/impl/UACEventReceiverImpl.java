package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.message.UACEvent;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

/**
 * Service implementation responsible for receipt of UAC Events. See Spring Integration flow for
 * details of in bound queue.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@MessageEndpoint
public class UACEventReceiverImpl {

  @Autowired private RespondentDataService respondentDataService;
  private static final Logger log = LoggerFactory.getLogger(UACEventReceiverImpl.class);

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param uacEvent UACEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptUACEvent")
  public void acceptUACEvent(UACEvent uacEvent) throws CTPException {

    UAC uac;
    String uacType = uacEvent.getEvent().getType();
    String uacTransactionId = uacEvent.getEvent().getTransactionId();

    log.with(uacType)
        .with(uacTransactionId)
        .info("Now receiving uac event with transactionId and type as shown here");

    uac = uacEvent.getPayload().getUac();

    try {
      respondentDataService.writeUAC(uac);
    } catch (CTPException ctpEx) {
      log.with(uacTransactionId)
          .with(ctpEx.getMessage())
          .error("ERROR: The event processing, for this transactionId, has failed");
      throw new CTPException(ctpEx.getFault());
    }
  }
}