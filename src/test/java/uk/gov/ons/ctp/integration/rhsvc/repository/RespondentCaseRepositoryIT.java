package uk.gov.ons.ctp.integration.rhsvc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;

public class RespondentCaseRepositoryIT extends FirestoreTestBase {
  private static final String CASE_ID = "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4";

  @Autowired private RespondentCaseRepository caseRepo;
  
  @BeforeEach
  public void setup() throws Exception {
    deleteAllCollections();
  }

  @Test
  public void shouldReadWriteCaseUpdate() throws Exception {
    assertTrue(caseRepo.readCaseUpdate(CASE_ID).isEmpty());

    CaseUpdate caze = FixtureHelper.loadPackageFixtures(CaseUpdate[].class).get(0);
    caseRepo.writeCaseUpdate(caze);

    Optional<CaseUpdate> retrieved = caseRepo.readCaseUpdate(CASE_ID);
    assertTrue(retrieved.isPresent());
    assertEquals(caze, retrieved.get());
  }
}
