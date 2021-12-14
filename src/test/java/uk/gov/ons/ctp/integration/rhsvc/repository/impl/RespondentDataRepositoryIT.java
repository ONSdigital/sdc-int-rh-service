package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;

public class RespondentDataRepositoryIT extends FirestoreTestBase {
  private static final String SURVEY_ID = "3883af91-0052-4497-9805-3238544fcf8a";
  private static final String COLLEX_ID = "44d7f3bb-91c9-45d0-bb2d-90afce4fc790";
  private static final String CASE_ID = "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4";
  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";

  @Autowired private RespondentSurveyRepository surveyRepo;
  @Autowired private RespondentCollectionExerciseRepository collExRepo;
  @Autowired private RespondentCaseRepository caseRepo;
  @Autowired private RespondentUacRepository uacRepo;
  
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
  public void shouldReadWriteUacUpdate() throws Exception {
    assertTrue(uacRepo.readUAC(UAC_HASH).isEmpty());

    UacUpdate uacUpdate = FixtureHelper.loadPackageFixtures(UacUpdate[].class).get(0);
    uacRepo.writeUAC(uacUpdate);

    Optional<UacUpdate> retrieved = uacRepo.readUAC(UAC_HASH);
    assertTrue(retrieved.isPresent());
    assertEquals(uacUpdate, retrieved.get());
  }

  @Test
  public void shouldReadWriteSurveyUpdate() throws Exception {
    assertTrue(surveyRepo.readSurvey(SURVEY_ID).isEmpty());

    SurveyUpdate survey = FixtureHelper.loadPackageFixtures(SurveyUpdate[].class).get(0);
    surveyRepo.writeSurvey(survey);

    Optional<SurveyUpdate> retrieved = surveyRepo.readSurvey(SURVEY_ID);
    assertTrue(retrieved.isPresent());
    assertEquals(survey, retrieved.get());
  }

  @Test
  public void shouldReadWriteCollectionExercise() throws Exception {
    assertTrue(collExRepo.readCollectionExercise(COLLEX_ID).isEmpty());

    CollectionExercise collex =
        FixtureHelper.loadPackageFixtures(CollectionExercise[].class).get(0);
    collExRepo.writeCollectionExercise(collex);

    Optional<CollectionExercise> retrieved = collExRepo.readCollectionExercise(COLLEX_ID);
    assertTrue(retrieved.isPresent());
    assertEquals(collex, retrieved.get());
  }
}
