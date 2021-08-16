package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.wildfly.common.Assert.assertTrue;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.SneakyThrows;
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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.concurrent.ListenableFuture;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.event.model.UacPayload;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.UACEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class, UacEventReceiverImplIT_Config.class})
@ExtendWith(SpringExtension.class)
@ActiveProfiles("mocked-connection-factory")
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class UacEventReceiverImplIT_Test {

  // TODO

  private static final String UPDATE_SAMPLE_SENSITIVE_TOPIC = "event_update-sample-sensitive";
  @Autowired private PubSubTemplate pubSubTemplate;
  @Autowired private UACEventReceiverImpl receiver;
  @MockBean private RespondentDataRepositoryImpl respondentDataRepo;

  @BeforeEach
  public void initMocks() {
    Mockito.reset(receiver);
  }

  @SneakyThrows
  private void UacEventFlow(EventType type) {
    UacEvent UacEvent = createUAC(RespondentHomeFixture.A_QID, type);

    // Construct message
    //      MessageProperties amqpMessageProperties = new MessageProperties();
    //      org.springframework.amqp.core.Message amqpMessage =
    //          new Jackson2JsonMessageConverter().toMessage(UacEvent, amqpMessageProperties);

    // Send message to container

    //      ChannelAwareMessageListener listener =
    //          (ChannelAwareMessageListener) UacEventListenerContainer.getMessageListener();
    //      final Channel rabbitChannel = mock(Channel.class);
    //      listener.onMessage(amqpMessage, rabbitChannel);

    sendMessage(UPDATE_SAMPLE_SENSITIVE_TOPIC, UacEvent);

    // Capture and check Service Activator argument
    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(UacEvent.getPayload()));
    verify(respondentDataRepo).writeUAC(any());
  }

  @Retryable(
      value = {java.io.IOException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 5000))
  public void sendMessage(String topicName, Object message) {
    ListenableFuture<String> future = pubSubTemplate.publish(topicName, message);

    try {
      future.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  /** Test the receiver flow for UAC created */
  @Test
  public void uacCreatedEventFlow() {
    UacEventFlow(EventType.UAC_UPDATE);
  }

  /** Test the receiver flow for UAC updated */
  @Test
  public void uacUpdatedEventFlow() {
    UacEventFlow(EventType.UAC_UPDATE);
  }
  //
  //  @Test
  //  public void shouldFilterUacEventWithContinuationFormQid() throws Exception {
  //
  //    UacEvent UacEvent = createUAC(RespondentHomeFixture.QID_12, EventType.UAC_UPDATED);
  //
  //    // Construct message
  //    MessageProperties amqpMessageProperties = new MessageProperties();
  //    org.springframework.amqp.core.Message amqpMessage =
  //        new Jackson2JsonMessageConverter().toMessage(UacEvent, amqpMessageProperties);
  //
  //    // Send message to container
  //    ChannelAwareMessageListener listener =
  //        (ChannelAwareMessageListener) UacEventListenerContainer.getMessageListener();
  //    final Channel rabbitChannel = mock(Channel.class);
  //    listener.onMessage(amqpMessage, rabbitChannel);
  //
  //    // Capture and check Service Activator argument
  //    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
  //    verify(receiver).acceptUacEvent(captur.capture());
  //    assertTrue(captur.getValue().getPayload().equals(UacEvent.getPayload()));
  //    verify(respondentDataRepo, never()).writeUAC(any());
  //  }

  //  @Test
  //  public void UacEventReceivedWithoutMillisecondsTest() throws Exception {
  //
  //    // Create a UAC with a timestamp. Note that the milliseconds are not specified
  //    UacEvent UacEvent = createUAC(RespondentHomeFixture.A_QID, EventType.UAC_UPDATED);
  //    String uac = new ObjectMapper().writeValueAsString(UacEvent);
  //    String uacWithTimestamp =
  //        uac.replaceAll("\"dateTime\":\"[^\"]*", "\"dateTime\":\"2011-08-12T20:17:46Z");
  //    assertTrue(uacWithTimestamp.contains("20:17:46Z"));
  //
  //    // Send message to container
  //    ChannelAwareMessageListener listener =
  //        (ChannelAwareMessageListener) UacEventListenerContainer.getMessageListener();
  //    final Channel rabbitChannel = mock(Channel.class);
  //    MessageProperties amqpMessageProperties = new MessageProperties();
  //    listener.onMessage(
  //        new Message(uacWithTimestamp.getBytes(), amqpMessageProperties), rabbitChannel);
  //
  //    // Capture and check Service Activator argument
  //    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
  //    verify(receiver).acceptUacEvent(captur.capture());
  //    assertTrue(captur.getValue().getPayload().equals(UacEvent.getPayload()));
  //    verify(respondentDataRepo).writeUAC(any());
  //  }

  private UacEvent createUAC(String qid, EventType type) {
    // Construct UacEvent
    UacEvent UacEvent = new UacEvent();
    UacPayload uacPayload = UacEvent.getPayload();
    UAC uac = uacPayload.getUac();
    uac.setUacHash("999999999");
    uac.setActive("true");
    uac.setQuestionnaireId(qid);
    uac.setCaseId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    Header header = new Header();
    header.setType(type);
    header.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    header.setDateTime(new Date());
    UacEvent.setEvent(header);
    return UacEvent;
  }
}
