package uk.gov.ons.ctp.integration.rhsvc.event;

import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;

/** Service implementation responsible for receipt of Collection Exercise Events. */
@MessageEndpoint
public interface CollectionExerciseEventReceiver {
  /**
   * Message end point for events from Response Management.
   *
   * @param event CollectionExerciseUpdateEvent message from Response Management
   * @throws CTPException something went wrong
   */
  public void acceptCollectionExerciseUpdateEvent(CollectionExerciseUpdateEvent event)
      throws CTPException;
}
