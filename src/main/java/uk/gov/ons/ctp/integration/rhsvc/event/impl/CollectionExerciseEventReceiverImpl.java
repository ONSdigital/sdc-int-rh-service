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
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.integration.rhsvc.repository.CollectionExerciseRepository;

/** Service implementation responsible for receipt of Collection Exercise Events. */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@MessageEndpoint
public class CollectionExerciseEventReceiverImpl {
  @Autowired private CollectionExerciseRepository respondentCollExRepo;

  /**
   * Message end point for events from Response Management.
   *
   * @param collectionExerciseUpdateEvent CollectionExerciseUpdateEvent message from Response
   *     Management
   * @throws CTPException something went wrong
   */
  @ServiceActivator(inputChannel = "acceptCollectionExerciseEvent")
  public void acceptCollectionExerciseUpdateEvent(
      CollectionExerciseUpdateEvent collectionExerciseUpdateEvent) throws CTPException {

    CollectionExerciseUpdate collectionExercise =
        collectionExerciseUpdateEvent.getPayload().getCollectionExerciseUpdate();
    String collexMessageId = collectionExerciseUpdateEvent.getHeader().getMessageId().toString();

    log.info(
        "Entering acceptCollectionExerciseUpdateEvent",
        kv("messageId", collexMessageId),
        kv("collectionExerciseId", collectionExercise.getCollectionExerciseId()));

    try {
      respondentCollExRepo.writeCollectionExercise(collectionExercise);
    } catch (CTPException ctpEx) {
      log.error(
          "Collection Exercise Event processing failed",
          kv("collectionExerciseMessageId", collexMessageId),
          ctpEx);
      throw ctpEx;
    }
  }
}
