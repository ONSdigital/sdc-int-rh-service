package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.FirestoreTestBase;
import uk.gov.ons.ctp.integration.rhsvc.event.CaseEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.repository.CaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;

public class CaseEventReceiverIT extends FirestoreTestBase {
  @Autowired private CaseEventReceiver receiver;
  @Autowired private SurveyRepository surveyRepo;
  @Autowired private CollectionExerciseRepository collExRepo;
  @Autowired private CaseRepository caseRepo;

  @BeforeEach
  public void setup() {
    deleteAllCollections();
  }

  @Test
  public void shouldReceiveCase() throws Exception {
    // setup survey and collex
    SurveyUpdate survey = FixtureHelper.loadPackageFixtures(SurveyUpdate[].class).get(0);
    surveyRepo.writeSurvey(survey);
    CollectionExercise collex =
        FixtureHelper.loadPackageFixtures(CollectionExercise[].class).get(0);
    collExRepo.writeCollectionExercise(collex);

    // now receive a case
    CaseEvent event = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    receiver.acceptCaseEvent(event);

    // verify case was added
    CaseUpdate sentCaseUpdate = event.getPayload().getCaseUpdate();
    Optional<CaseUpdate> retrieved = caseRepo.readCaseUpdate(sentCaseUpdate.getCaseId());
    assertTrue(retrieved.isPresent());
    assertEquals(sentCaseUpdate, retrieved.get());
  }

  @Test
  public void shouldIgnoreReceiveCaseForSurveyNotPresent() throws Exception {
    CaseEvent event = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    receiver.acceptCaseEvent(event);

    CaseUpdate sentCaseUpdate = event.getPayload().getCaseUpdate();
    Optional<CaseUpdate> retrieved = caseRepo.readCaseUpdate(sentCaseUpdate.getCaseId());
    assertFalse(retrieved.isPresent());
  }
}
