package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentCollectionExerciseRepository;

@ExtendWith(MockitoExtension.class)
public class CollectionExerciseEventReceiverImplUnit_test {

  @Mock private RespondentCollectionExerciseRepository mockRespondentCollExRepo;

  @InjectMocks private CollectionExerciseEventReceiverImpl target;

  @Test
  public void test_acceptCollectionExerciseEvent_success() throws Exception {
    CollectionExerciseUpdateEvent collectionExerciseUpdateEvent =
        FixtureHelper.loadPackageFixtures(CollectionExerciseUpdateEvent[].class).get(0);
    target.acceptCollectionExerciseUpdateEvent(collectionExerciseUpdateEvent);
    verify(mockRespondentCollExRepo)
        .writeCollectionExercise(
            collectionExerciseUpdateEvent.getPayload().getCollectionExerciseUpdate());
  }

  @Test
  public void test_acceptCollectionExerciseEvent_exceptionThrown() throws Exception {
    CollectionExerciseUpdateEvent collectionExerciseUpdateEvent =
        FixtureHelper.loadPackageFixtures(CollectionExerciseUpdateEvent[].class).get(0);
    Mockito.doThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND))
        .when(mockRespondentCollExRepo)
        .writeCollectionExercise(
            collectionExerciseUpdateEvent.getPayload().getCollectionExerciseUpdate());
    assertThrows(
        CTPException.class,
        () -> target.acceptCollectionExerciseUpdateEvent(collectionExerciseUpdateEvent));
  }
}
