package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.Mockito.verify;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.CaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.UacRepository;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class, MessageSpringConfig.class})
@ExtendWith(SpringExtension.class)
@ActiveProfiles("mocked-connection-factory")
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class CollectionExerciseEventReceiverImplSpringTest {

  @Autowired private CollectionExerciseEventReceiverImpl receiver;
  @Autowired private PubSubInboundChannelAdapter collectionExerciseEventInbound;
  @MockBean private SurveyRepository respondentSurveyRepo;
  @MockBean private CollectionExerciseRepository respondentCollExRepo;
  @MockBean private CaseRepository respondentCaseRepo;
  @MockBean private UacRepository respondentUacRepo;
  @MockBean private PubSubTemplate pubSubTemplate;
  @MockBean private EventFilter eventFilter;

  @BeforeEach
  public void initMocks() {
    Mockito.reset(receiver);
  }

  /** Test the receiver flow */
  @Test
  public void collectionExerciseEventFlowTest() throws Exception {
    CollectionExerciseUpdateEvent collectionExerciseUpdateEvent =
        FixtureHelper.loadPackageFixtures(CollectionExerciseUpdateEvent[].class).get(0);

    // Construct message
    Message<CollectionExerciseUpdateEvent> message =
        new GenericMessage<>(collectionExerciseUpdateEvent, new HashMap<>());
    // Send message to container
    collectionExerciseEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<CollectionExerciseUpdateEvent> captur =
        ArgumentCaptor.forClass(CollectionExerciseUpdateEvent.class);
    verify(receiver).acceptCollectionExerciseUpdateEvent(captur.capture());
    assertEquals(captur.getValue().getPayload(), collectionExerciseUpdateEvent.getPayload());
  }

  @Test
  public void collectionExerciseEventReceivedWithoutMillisTest() throws Exception {

    // Create a collection exercise with a timestamp. Note that the milliseconds are not specified
    CollectionExerciseUpdateEvent collectionExerciseUpdateEvent =
        FixtureHelper.loadPackageFixtures(CollectionExerciseUpdateEvent[].class).get(0);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    collectionExerciseUpdateEvent.getHeader().setDateTime(sdf.parse("2011-08-12T20:17:46Z"));

    // Construct message
    Message<CollectionExerciseUpdateEvent> message =
        new GenericMessage<>(collectionExerciseUpdateEvent, new HashMap<>());

    // Send message to container
    collectionExerciseEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<CollectionExerciseUpdateEvent> captur =
        ArgumentCaptor.forClass(CollectionExerciseUpdateEvent.class);
    verify(receiver).acceptCollectionExerciseUpdateEvent(captur.capture());
    assertEquals(sdf.parse("2011-08-12T20:17:46Z"), captur.getValue().getHeader().getDateTime());
    assertEquals(collectionExerciseUpdateEvent.getHeader(), captur.getValue().getHeader());
    assertEquals(collectionExerciseUpdateEvent.getPayload(), captur.getValue().getPayload());
  }
}
