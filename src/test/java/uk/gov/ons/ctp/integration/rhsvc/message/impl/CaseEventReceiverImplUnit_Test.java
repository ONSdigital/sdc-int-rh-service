package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CaseEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

@ExtendWith(MockitoExtension.class)
public class CaseEventReceiverImplUnit_Test {

  @Mock private RespondentDataRepository mockRespondentDataRepo;

  @InjectMocks private CaseEventReceiverImpl target;

  @Test
  public void test_acceptCaseEvent_success() throws Exception {
    Header header = new Header();
    header.setMessageId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    caseEvent.setHeader(header);
    target.acceptCaseEvent(caseEvent);
    verify(mockRespondentDataRepo).writeCollectionCase(caseEvent.getPayload().getCollectionCase());
  }
}
