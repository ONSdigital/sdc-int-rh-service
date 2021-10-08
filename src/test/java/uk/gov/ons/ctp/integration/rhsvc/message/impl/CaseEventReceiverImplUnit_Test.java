package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CaseEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

@ExtendWith(MockitoExtension.class)
public class CaseEventReceiverImplUnit_Test {

  @Mock private RespondentDataRepository mockRespondentDataRepo;

  @InjectMocks private CaseEventReceiverImpl target;

  @Test
  public void test_acceptCaseEvent_success() throws Exception {
    Header header = new Header();
    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    SurveyUpdate surveyUpdate = new SurveyUpdate("1", "NOTSIS");
    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    caseEvent.setHeader(header);
    target.acceptCaseEvent(caseEvent);
    verify(mockRespondentDataRepo).writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
  }

  @Test
  public void test_acceptCaseEvent_reject_SIS() throws Exception {
    Header header = new Header();
    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    caseEvent.setHeader(header);
    SurveyUpdate surveyUpdate = new SurveyUpdate("1", "SIS");
    when(mockRespondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    target.acceptCaseEvent(caseEvent);
    verify(mockRespondentDataRepo, times(0))
        .writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
  }
}
