package uk.gov.ons.ctp.integration.rhsvc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;

public class CaseRepositoryIT extends FirestoreTestBase {
  private static final String CASE_ID = "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4";

  @Autowired private CaseRepository caseRepo;

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

  @Test
  public void shouldFindCasesByAttribute() throws Exception {
    List<CaseUpdate> cases = FixtureHelper.loadPackageFixtures(CaseUpdate[].class);
    setCaseData(cases.get(0), "SO145AA", "CC1");
    setCaseData(cases.get(1), "SO145AA", "CC4");
    setCaseData(cases.get(2), "SO145AA", "CC1");

    caseRepo.writeCaseUpdate(cases.get(0));
    caseRepo.writeCaseUpdate(cases.get(1));
    caseRepo.writeCaseUpdate(cases.get(2));

    // Find all cases, as the all match by postcode
    List<CaseUpdate> foundByPostcode =
        caseRepo.findCaseUpdatesBySampleAttribute("postcode", "SO145AA", false);
    assertEquals(cases, foundByPostcode);

    // Find subset of cases
    List<CaseUpdate> foundByCohort =
        caseRepo.findCaseUpdatesBySampleAttribute("cohort", "CC1", false);
    List<CaseUpdate> cohortCases = List.of(cases.get(0), cases.get(2));
    assertEquals(cohortCases, foundByCohort);
  }

  @Test
  public void shouldFindZeroCasesByAttribute() throws Exception {
    List<CaseUpdate> cases = FixtureHelper.loadPackageFixtures(CaseUpdate[].class);
    caseRepo.writeCaseUpdate(cases.get(0));

    // Do a search which should find no matching cases
    List<CaseUpdate> foundCases =
        caseRepo.findCaseUpdatesBySampleAttribute("passportNumber", "4343333", false);
    assertEquals(0, foundCases.size());
  }

  private void setCaseData(CaseUpdate caseUpdate, String postcode, String cohort) {
    caseUpdate.getSample().put("postcode", postcode);
    caseUpdate.getSample().put("cohort", cohort);
  }
}
