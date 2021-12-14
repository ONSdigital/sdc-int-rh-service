package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentCaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentCollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentSurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

public class CaseServiceIT extends FirestoreTestBase {
  @Autowired private RespondentSurveyRepository surveyRepo;
  @Autowired private RespondentCollectionExerciseRepository collExRepo;
  @Autowired private RespondentCaseRepository caseRepo;
  @Autowired private CaseService service;

  @BeforeEach
  public void setup() {
    deleteAllCollections();
  }

  private void createCase(CaseUpdate caze, UniquePropertyReferenceNumber uprn) throws Exception {
    // setup survey and collex
    SurveyUpdate survey = FixtureHelper.loadPackageFixtures(SurveyUpdate[].class).get(0);
    survey.setSurveyId(caze.getSurveyId());
    surveyRepo.writeSurvey(survey);
    CollectionExercise collex =
        FixtureHelper.loadPackageFixtures(CollectionExercise[].class).get(0);
    collex.setSurveyId(survey.getSurveyId());
    collex.setCollectionExerciseId(caze.getCollectionExerciseId());
    collExRepo.writeCollectionExercise(collex);

    // create Case
    caze.getSample().put("uprn", Long.toString(uprn.getValue()));
    caseRepo.writeCaseUpdate(caze);
  }

  @Test
  public void shouldFindCaseByUprn() throws Exception {
    UniquePropertyReferenceNumber uprn = UniquePropertyReferenceNumber.create("100040226442");
    CaseUpdate caze = FixtureHelper.loadPackageFixtures(CaseUpdate[].class).get(0);
    createCase(caze, uprn);

    List<CaseDTO> dto = service.readCaseUpdateBySampleAttribute("uprn", Long.toString(uprn.getValue()));
    assertNotNull(dto);
    assertEquals(caze.getCaseId(), dto.get(0).getCaseId().toString());
    assertEquals(caze.getCaseRef(), dto.get(0).getCaseRef());
    assertEquals(caze.getSample().get("addressLine1"), dto.get(0).getSample().get("addressLine1"));
    assertEquals(1, dto.size());
  }

  @Test
  public void shouldNotFindCaseForUnknownUprn() throws Exception {
    UniquePropertyReferenceNumber uprn = UniquePropertyReferenceNumber.create("1000666");
    List<CaseDTO> cases = service.readCaseUpdateBySampleAttribute("uprn", Long.toString(uprn.getValue()));
    assertTrue(cases.isEmpty());
  }
}
