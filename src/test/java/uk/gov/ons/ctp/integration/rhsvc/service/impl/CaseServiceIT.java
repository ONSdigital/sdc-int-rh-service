package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

public class CaseServiceIT extends FirestoreTestBase {
  @Autowired private RespondentDataRepository repo;
  @Autowired private CaseService service;

  @BeforeEach
  public void setup() {
    deleteAllCollections();
  }

  private void createCase(CaseUpdate caze, UniquePropertyReferenceNumber uprn) throws Exception {
    // setup survey and collex
    SurveyUpdate survey = FixtureHelper.loadPackageFixtures(SurveyUpdate[].class).get(0);
    survey.setSurveyId(caze.getSurveyId());
    repo.writeSurvey(survey);
    CollectionExercise collex =
        FixtureHelper.loadPackageFixtures(CollectionExercise[].class).get(0);
    collex.setSurveyId(survey.getSurveyId());
    collex.setCollectionExerciseId(caze.getCollectionExerciseId());
    repo.writeCollectionExercise(collex);

    // create Case
    caze.getSample().setUprn(Long.toString(uprn.getValue()));
    repo.writeCaseUpdate(caze);
  }

  @Test
  public void shouldFindCaseByUprn() throws Exception {
    UniquePropertyReferenceNumber uprn = UniquePropertyReferenceNumber.create("100040226442");
    CaseUpdate caze = FixtureHelper.loadPackageFixtures(CaseUpdate[].class).get(0);
    createCase(caze, uprn);

    CaseDTO dto = service.getLatestValidCaseByUPRN(uprn);
    assertNotNull(dto);
    assertEquals(caze.getCaseId(), dto.getCaseId().toString());
    assertEquals(caze.getCaseRef(), dto.getCaseRef());
    assertEquals(caze.getSample().getAddressLine1(), dto.getAddress().getAddressLine1());
  }

  @Test
  public void shouldHandleFailureToFindCaseByUprn() throws Exception {
    UniquePropertyReferenceNumber uprn = UniquePropertyReferenceNumber.create("100040226442");
    CTPException e = assertThrows(CTPException.class, () -> service.getLatestValidCaseByUPRN(uprn));
    assertEquals(Fault.RESOURCE_NOT_FOUND, e.getFault());
  }
}
