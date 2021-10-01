package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.wildfly.common.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
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
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ctp.common.event.EventTopic;
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
@ContextConfiguration(classes = {AppConfig.class, MessageIT_Config.class})
@ExtendWith(SpringExtension.class)
@ActiveProfiles("mocked-connection-factory")
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class UacEventReceiverImplIT_Test {

  @Autowired private PubSubInboundChannelAdapter uacEventInbound;
  @Autowired private AppConfig appConfig;
  @Autowired private UACEventReceiverImpl receiver;
  @MockBean private PubSubTemplate pubSubTemplate;
  @MockBean private RespondentDataRepositoryImpl respondentDataRepo;

  @BeforeEach
  public void initMocks() {
    Mockito.reset(receiver);
  }

  @SneakyThrows
  private void UacEventFlow(EventTopic topic) {
    UacEvent UacEvent = createUAC(RespondentHomeFixture.A_QID, topic);

    // Construct message
    Message<UacEvent> message = new GenericMessage<>(UacEvent, new HashMap<>());
    // Send message to container
    uacEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(UacEvent.getPayload()));
    verify(respondentDataRepo).writeUAC(any());
  }

  /** Test the receiver flow for UAC updated */
  @Test
  public void uacUpdatedEventFlow() {
    UacEventFlow(EventTopic.UAC_UPDATE);
  }

  @Test
  public void shouldFilterUacEventWithContinuationFormQid() throws Exception {

    appConfig.getQueueConfig().setQidFilterPrefixes(Set.of("12"));
    UacEvent UacEvent = createUAC(RespondentHomeFixture.QID_12, EventTopic.UAC_UPDATE);

    // Construct message
    Message<UacEvent> message = new GenericMessage<>(UacEvent, new HashMap<>());
    // Send message to container
    uacEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(UacEvent.getPayload()));
    verify(respondentDataRepo, never()).writeUAC(any());
    verify(respondentDataRepo, never()).writeUAC(any());
  }

  @Test
  public void UacEventReceivedWithoutMillisecondsTest() throws Exception {

    // Create a UAC with a timestamp. Note that the milliseconds are not specified
    UacEvent UacEvent = createUAC(RespondentHomeFixture.A_QID, EventTopic.UAC_UPDATE);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    UacEvent.getHeader().setDateTime(sdf.parse("2011-08-12T20:17:46Z"));

    // Construct message
    Message<UacEvent> message = new GenericMessage<>(UacEvent, new HashMap<>());
    // Send message to container
    uacEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertEquals(sdf.parse("2011-08-12T20:17:46Z"), captur.getValue().getHeader().getDateTime());
    assertEquals(UacEvent.getHeader(), captur.getValue().getHeader());
    assertTrue(captur.getValue().getPayload().equals(UacEvent.getPayload()));
    verify(respondentDataRepo).writeUAC(any());
  }

  private UacEvent createUAC(String qid, EventTopic topic) {
    // Construct UacEvent
    UacEvent UacEvent = new UacEvent();
    UacPayload uacPayload = UacEvent.getPayload();
    UAC uac = uacPayload.getUac();
    uac.setUacHash("999999999");
    uac.setActive("true");
    uac.setQuestionnaireId(qid);
    uac.setCaseId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    Header header = new Header();
    header.setTopic(topic);
    header.setMessageId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    header.setDateTime(new Date());
    UacEvent.setHeader(header);
    return UacEvent;
  }
}
