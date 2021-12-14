package uk.gov.ons.ctp.integration.rhsvc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;

@ExtendWith(MockitoExtension.class)
public class RespondentSurveyRepositoryTest {

  @Spy private RetryableCloudDataStore mockCloudDataStore;

  @InjectMocks private RespondentSurveyRepository surveyRepo;

  @Test
  public void shouldListSurveys() throws Exception {
    var surveys = FixtureHelper.loadClassFixtures(SurveyUpdate[].class);
    when(mockCloudDataStore.list(eq(SurveyUpdate.class), any())).thenReturn(surveys);
    var listedSurveys = surveyRepo.listSurveys();
    assertEquals(3, listedSurveys.size());
  }
}
