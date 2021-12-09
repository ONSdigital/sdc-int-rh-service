package uk.gov.ons.ctp.integration.rhsvc.message.impl;

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
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.EventFilter;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.SurveyEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentCaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentCollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentSurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentUacRepository;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class, MessageIT_Config.class})
@ExtendWith(SpringExtension.class)
@ActiveProfiles("mocked-connection-factory")
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class SurveyUpdateEventReceiverImplIT_Test {

  @Autowired private SurveyEventReceiverImpl receiver;
  @Autowired private PubSubInboundChannelAdapter surveyEventInbound;
  @MockBean private RespondentSurveyRepository respondentSurveyRepo;
  @MockBean private RespondentCollectionExerciseRepository respondentCollExRepo;
  @MockBean private RespondentCaseRepository respondentCaseRepo;
  @MockBean private RespondentUacRepository respondentUacRepo;
  @MockBean private PubSubTemplate pubSubTemplate;
  @MockBean private EventFilter eventFilter;

  @BeforeEach
  public void initMocks() {
    Mockito.reset(receiver);
  }

  /** Test the receiver flow */
  @Test
  public void surveyEventFlowTest() throws Exception {
    SurveyUpdateEvent surveyUpdateEvent =
        FixtureHelper.loadPackageFixtures(SurveyUpdateEvent[].class).get(0);

    // Construct message
    Message<SurveyUpdateEvent> message = new GenericMessage<>(surveyUpdateEvent, new HashMap<>());
    // Send message to container
    surveyEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<SurveyUpdateEvent> captur = ArgumentCaptor.forClass(SurveyUpdateEvent.class);
    verify(receiver).acceptSurveyUpdateEvent(captur.capture());
    assertEquals(captur.getValue().getPayload(), surveyUpdateEvent.getPayload());
  }

  @Test
  public void surveyEventReceivedWithoutMillisTest() throws Exception {

    // Create a Survey with a timestamp. Note that the milliseconds are not specified
    SurveyUpdateEvent surveyUpdateEvent =
        FixtureHelper.loadPackageFixtures(SurveyUpdateEvent[].class).get(0);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    surveyUpdateEvent.getHeader().setDateTime(sdf.parse("2011-08-12T20:17:46Z"));

    // Construct message
    Message<SurveyUpdateEvent> message = new GenericMessage<>(surveyUpdateEvent, new HashMap<>());

    // Send message to container
    surveyEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<SurveyUpdateEvent> captur = ArgumentCaptor.forClass(SurveyUpdateEvent.class);
    verify(receiver).acceptSurveyUpdateEvent(captur.capture());
    assertEquals(sdf.parse("2011-08-12T20:17:46Z"), captur.getValue().getHeader().getDateTime());
    assertEquals(surveyUpdateEvent.getHeader(), captur.getValue().getHeader());
    assertEquals(surveyUpdateEvent.getPayload(), captur.getValue().getPayload());
  }
}
