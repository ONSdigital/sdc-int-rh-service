package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.Mockito.verify;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CaseEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class, MessageIT_Config.class})
@ExtendWith(SpringExtension.class)
@ActiveProfiles("mocked-connection-factory")
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class CaseEventReceiverImplIT_Test {

  @Autowired private CaseEventReceiverImpl receiver;
  @Autowired private PubSubInboundChannelAdapter caseEventInbound;
  @MockBean private RespondentDataRepositoryImpl respondentDataRepo;
  @MockBean private PubSubTemplate pubSubTemplate;

  @BeforeEach
  public void initMocks() {
    Mockito.reset(receiver);
  }

  /** Test the receiver flow */
  @Test
  public void caseEventFlowTest() throws Exception {
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);

    // Construct message
    Message<CaseEvent> message = new GenericMessage<>(caseEvent, new HashMap<>());
    // Send message to container
    caseEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    verify(receiver).acceptCaseEvent(captur.capture());
    assertEquals(captur.getValue().getPayload(), caseEvent.getPayload());
  }

  @Test
  public void caseCaseReceivedWithoutMillisTest() throws Exception {

    // Create a case with a timestamp. Note that the milliseconds are not specified
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    caseEvent.getEvent().setDateTime(sdf.parse("2011-08-12T20:17:46Z"));

    // Construct message
    Message<CaseEvent> message = new GenericMessage<>(caseEvent, new HashMap<>());

    // Send message to container
    caseEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    verify(receiver).acceptCaseEvent(captur.capture());
    assertEquals(sdf.parse("2011-08-12T20:17:46Z"), captur.getValue().getEvent().getDateTime());
    assertEquals(caseEvent.getEvent(), captur.getValue().getEvent());
    assertEquals(caseEvent.getPayload(), captur.getValue().getPayload());
  }
}
