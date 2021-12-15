package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;

@ExtendWith(MockitoExtension.class)
public class SurveyUpdateEventReceiverImplUnit_test {

  @Mock private SurveyRepository mockRespondentSurveyRepo;

  @InjectMocks private SurveyEventReceiverImpl target;

  @Test
  public void test_acceptSurveyEvent_success() throws Exception {
    SurveyUpdateEvent surveyUpdateEvent =
        FixtureHelper.loadPackageFixtures(SurveyUpdateEvent[].class).get(0);
    target.acceptSurveyUpdateEvent(surveyUpdateEvent);
    verify(mockRespondentSurveyRepo).writeSurvey(surveyUpdateEvent.getPayload().getSurveyUpdate());
  }

  @Test
  public void test_acceptSurveyEvent_exceptionThrown() throws Exception {
    SurveyUpdateEvent surveyUpdateEvent =
        FixtureHelper.loadPackageFixtures(SurveyUpdateEvent[].class).get(0);
    Mockito.doThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND))
        .when(mockRespondentSurveyRepo)
        .writeSurvey(surveyUpdateEvent.getPayload().getSurveyUpdate());
    assertThrows(CTPException.class, () -> target.acceptSurveyUpdateEvent(surveyUpdateEvent));
  }
}
