package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CollectionExerciseEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

@ExtendWith(MockitoExtension.class)
public class CollectionExerciseEventReceiverImplUnit_test {

  @Mock private RespondentDataRepository mockRespondentDataRepo;

  @InjectMocks private CollectionExerciseEventReceiverImpl target;

  @Test
  public void test_acceptCollectionExerciseEvent_success() throws Exception {
    CollectionExerciseUpdateEvent collectionExerciseUpdateEvent =
        FixtureHelper.loadPackageFixtures(CollectionExerciseUpdateEvent[].class).get(0);
    target.acceptCollectionExerciseUpdateEvent(collectionExerciseUpdateEvent);
    verify(mockRespondentDataRepo)
        .writeCollectionExercise(
            collectionExerciseUpdateEvent.getPayload().getCollectionExerciseUpdate());
  }
}
