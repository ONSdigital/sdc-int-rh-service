package uk.gov.ons.ctp.integration.rhsvc.event;

import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;

/** Service implementation responsible for receipt of Survey Events. */
@MessageEndpoint
public interface SurveyEventReceiver {

  /**
   * Message end point for events from Response Management.
   *
   * @param event SurveyUpdateEvent message from Response Management
   * @throws CTPException something went wrong
   */
  public void acceptSurveyUpdateEvent(SurveyUpdateEvent event) throws CTPException;
}
