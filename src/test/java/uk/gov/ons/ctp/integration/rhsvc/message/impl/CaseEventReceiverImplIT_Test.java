package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class, CaseEventReceiverImplIT_Config.class})
@ExtendWith(SpringExtension.class)
@ActiveProfiles("mocked-connection-factory")
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class CaseEventReceiverImplIT_Test {

  // TODO

  //  @Autowired private CaseEventReceiverImpl receiver;
  //  @Autowired private SimpleMessageListenerContainer caseEventListenerContainer;
  //  @MockBean private RespondentDataRepositoryImpl respondentDataRepo;
  //
  //  @BeforeEach
  //  public void initMocks() {
  //    Mockito.reset(receiver);
  //  }
  //
  //  /** Test the receiver flow */
  //  @Test
  //  public void caseEventFlowTest() throws Exception {
  //    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
  //
  //    // Construct message
  //    MessageProperties amqpMessageProperties = new MessageProperties();
  //    org.springframework.amqp.core.Message amqpMessage =
  //        new Jackson2JsonMessageConverter().toMessage(caseEvent, amqpMessageProperties);
  //
  //    // Send message to container
  //    ChannelAwareMessageListener listener =
  //        (ChannelAwareMessageListener) caseEventListenerContainer.getMessageListener();
  //    final Channel rabbitChannel = mock(Channel.class);
  //    listener.onMessage(amqpMessage, rabbitChannel);
  //
  //    // Capture and check Service Activator argument
  //    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
  //    verify(receiver).acceptCaseEvent(captur.capture());
  //    assertTrue(captur.getValue().getPayload().equals(caseEvent.getPayload()));
  //  }

  //  @Test
  //  public void caseCaseReceivedWithoutMillisTest() throws Exception {
  //
  //    // Create a case with a timestamp. Note that the milliseconds are not specified
  //    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
  //
  //    String caseAsJson = new ObjectMapper().writeValueAsString(caseEvent);
  //    String caseWithoutMillis =
  //        caseAsJson.replaceAll("\"dateTime\":\"[^\"]*", "\"dateTime\":\"2011-08-12T20:17:46Z");
  //    assertTrue(caseWithoutMillis.contains("20:17:46Z"));
  //
  //    // Send message to container
  //    ChannelAwareMessageListener listener =
  //        (ChannelAwareMessageListener) caseEventListenerContainer.getMessageListener();
  //    final Channel rabbitChannel = mock(Channel.class);
  //    listener.onMessage(
  //        new Message(caseWithoutMillis.getBytes(), new MessageProperties()), rabbitChannel);
  //
  //    // Capture and check Service Activator argument
  //    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
  //    verify(receiver).acceptCaseEvent(captur.capture());
  //    assertTrue(captur.getValue().getPayload().equals(caseEvent.getPayload()));
  //  }
}
