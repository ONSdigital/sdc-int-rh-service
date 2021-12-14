package uk.gov.ons.ctp.integration.rhsvc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;

public class RespondentSurveyRepositoryIT extends FirestoreTestBase {
  private static final String SURVEY_ID = "3883af91-0052-4497-9805-3238544fcf8a";

  @Autowired private RespondentSurveyRepository surveyRepo;

  @BeforeEach
  public void setup() throws Exception {
    deleteAllCollections();
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
}
