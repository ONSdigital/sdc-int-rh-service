package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wildfly.common.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;

import lombok.SneakyThrows;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.EventTopic;
import uk.gov.ons.ctp.common.event.model.UacEvent;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentCaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentCollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentSurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentUacRepository;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class, MessageSpringConfig.class})
@ExtendWith(SpringExtension.class)
@ActiveProfiles("mocked-connection-factory")
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class UacEventReceiverImplSpringTest {

  @Autowired private PubSubInboundChannelAdapter uacEventInbound;
  @Autowired private AppConfig appConfig;
  @Autowired private UACEventReceiverImpl receiver;
  @MockBean private PubSubTemplate pubSubTemplate;
  @MockBean private RespondentSurveyRepository respondentSurveyRepo;
  @MockBean private RespondentCollectionExerciseRepository respondentCollExRepo;
  @MockBean private RespondentCaseRepository respondentCaseRepo;
  @MockBean private RespondentUacRepository respondentUacRepo;
  @MockBean private EventFilter eventFilter;

  @BeforeEach
  public void initMocks() {
    Mockito.reset(receiver);
  }

  @SneakyThrows
  private void UacEventFlow(EventTopic topic) {
    UacEvent uacEvent = FixtureHelper.loadPackageFixtures(UacEvent[].class).get(0);
    uacEvent.getPayload().getUacUpdate().setQid(RespondentHomeFixture.A_QID);

    // Construct message
    Message<UacEvent> message = new GenericMessage<>(uacEvent, new HashMap<>());

    when(eventFilter.isValidEvent(any(), any(), any(), any())).thenReturn(true);

    // Send message to container
    uacEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(uacEvent.getPayload()));
    verify(respondentUacRepo).writeUAC(any());
  }

  /** Test the receiver flow for UAC updated */
  @Test
  public void uacUpdatedEventFlow() {
    UacEventFlow(EventTopic.UAC_UPDATE);
  }

  @Test
  public void shouldFilterUacEventWithContinuationFormQid() throws Exception {

    appConfig.getQueueConfig().setQidFilterPrefixes(Set.of("12"));
    UacEvent uacEvent = FixtureHelper.loadPackageFixtures(UacEvent[].class).get(0);
    uacEvent.getPayload().getUacUpdate().setQid(RespondentHomeFixture.QID_12);

    // Construct message
    Message<UacEvent> message = new GenericMessage<>(uacEvent, new HashMap<>());

    when(eventFilter.isValidEvent(any(), any(), any(), any())).thenReturn(true);

    // Send message to container
    uacEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(uacEvent.getPayload()));
    verify(respondentUacRepo, never()).writeUAC(any());
    verify(respondentUacRepo, never()).writeUAC(any());
  }

  @Test
  public void UacEventReceivedWithoutMillisecondsTest() throws Exception {

    // Create a UAC with a timestamp. Note that the milliseconds are not specified
    UacEvent uacEvent = FixtureHelper.loadPackageFixtures(UacEvent[].class).get(0);
    uacEvent.getPayload().getUacUpdate().setQid(RespondentHomeFixture.A_QID);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    uacEvent.getHeader().setDateTime(sdf.parse("2011-08-12T20:17:46Z"));

    // Construct message
    Message<UacEvent> message = new GenericMessage<>(uacEvent, new HashMap<>());

    when(eventFilter.isValidEvent(any(), any(), any(), any())).thenReturn(true);
    // Send message to container
    uacEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<UacEvent> captur = ArgumentCaptor.forClass(UacEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertEquals(sdf.parse("2011-08-12T20:17:46Z"), captur.getValue().getHeader().getDateTime());
    assertEquals(uacEvent.getHeader(), captur.getValue().getHeader());
    assertTrue(captur.getValue().getPayload().equals(uacEvent.getPayload()));
    verify(respondentUacRepo).writeUAC(any());
  }
}
