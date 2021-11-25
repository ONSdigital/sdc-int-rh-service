package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CaseEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.util.AcceptedEventFilter;

@ExtendWith(MockitoExtension.class)
public class CaseEventReceiverImplUnit_Test {

  @Mock private RespondentDataRepository mockRespondentDataRepo;

  @Mock private AcceptedEventFilter acceptedEventFilter;

  @InjectMocks private CaseEventReceiverImpl target;

  //TODO saveValidCase() {}
  //doNotSaveInvalidCase() {}

  @Test
  public void test_successfulFilter_caseSaved() throws Exception {
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    when(acceptedEventFilter.filterAcceptedEvents(any(), any(), any(), any())).thenReturn(true);

    target.acceptCaseEvent(caseEvent);

    verify(mockRespondentDataRepo).writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
  }

  @Test
  public void test_unsucessfulFilter_caseRejected() throws Exception {
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    when(acceptedEventFilter.filterAcceptedEvents(any(), any(), any(), any())).thenReturn(false);

    target.acceptCaseEvent(caseEvent);

    verify(mockRespondentDataRepo, times(0)).writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
  }

}
