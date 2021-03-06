package uk.gov.ons.ctp.integration.rhsvc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;

public class CollectionExcerciseRepositoryIT extends FirestoreTestBase {
  private static final String COLLEX_ID = "44d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  @Autowired private CollectionExerciseRepository collExRepo;

  @BeforeEach
  public void setup() throws Exception {
    deleteAllCollections();
  }

  @Test
  public void shouldReadWriteCollectionExercise() throws Exception {
    assertTrue(collExRepo.readCollectionExercise(COLLEX_ID).isEmpty());

    CollectionExerciseUpdate collex =
        FixtureHelper.loadPackageFixtures(CollectionExerciseUpdate[].class).get(0);
    collExRepo.writeCollectionExercise(collex);

    Optional<CollectionExerciseUpdate> retrieved = collExRepo.readCollectionExercise(COLLEX_ID);
    assertTrue(retrieved.isPresent());
    assertEquals(collex, retrieved.get());
  }
}
