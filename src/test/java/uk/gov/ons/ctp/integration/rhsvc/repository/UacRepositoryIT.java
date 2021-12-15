package uk.gov.ons.ctp.integration.rhsvc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;

public class UacRepositoryIT extends FirestoreTestBase {
  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";

  @Autowired private UacRepository uacRepo;

  @BeforeEach
  public void setup() throws Exception {
    deleteAllCollections();
  }

  @Test
  public void shouldReadWriteUacUpdate() throws Exception {
    assertTrue(uacRepo.readUAC(UAC_HASH).isEmpty());

    UacUpdate uacUpdate = FixtureHelper.loadPackageFixtures(UacUpdate[].class).get(0);
    uacRepo.writeUAC(uacUpdate);

    Optional<UacUpdate> retrieved = uacRepo.readUAC(UAC_HASH);
    assertTrue(retrieved.isPresent());
    assertEquals(uacUpdate, retrieved.get());
  }
}
