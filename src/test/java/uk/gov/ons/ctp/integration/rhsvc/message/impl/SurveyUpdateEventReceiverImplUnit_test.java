package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.SurveyEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

@ExtendWith(MockitoExtension.class)
public class SurveyUpdateEventReceiverImplUnit_test {

  @Mock private RespondentDataRepository mockRespondentDataRepo;

  @InjectMocks private SurveyEventReceiverImpl target;

  @Test
  public void test_acceptSurveyEvent_success() throws Exception {
    SurveyUpdateEvent surveyUpdateEvent =
        FixtureHelper.loadPackageFixtures(SurveyUpdateEvent[].class).get(0);
    target.acceptSurveyUpdateEvent(surveyUpdateEvent);
    verify(mockRespondentDataRepo).writeSurvey(surveyUpdateEvent.getPayload().getSurveyUpdate());
  }
}