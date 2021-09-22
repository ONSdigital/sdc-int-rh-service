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
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/** Service implementation responsible for receipt of Collection Exercise Events. */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@MessageEndpoint
public class CollectionExerciseEventReceiverImpl {
  @Autowired private RespondentDataRepository respondentDataRepo;

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

    CollectionExercise collectionExercise =
        collectionExerciseUpdateEvent.getPayload().getCollectionExercise();
    String collexTransactionId = collectionExerciseUpdateEvent.getEvent().getTransactionId();

    log.info(
        "Entering acceptCollectionExerciseUpdateEvent",
        kv("transactionId", collexTransactionId),
        kv("collectionExerciseId", collectionExercise.getId()));

    try {
      respondentDataRepo.writeCollectionExercise(collectionExercise);
    } catch (CTPException ctpEx) {
      log.error(
          "Collection Exercise Event processing failed",
          kv("collectionExerciseTransactionId", collexTransactionId),
          ctpEx);
      throw new CTPException(ctpEx.getFault());
    }
  }
}
