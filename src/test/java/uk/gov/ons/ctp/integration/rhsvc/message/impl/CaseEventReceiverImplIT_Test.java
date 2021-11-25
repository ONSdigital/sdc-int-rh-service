package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
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
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CaseEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;
import uk.gov.ons.ctp.integration.rhsvc.util.AcceptableEventFilter;

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
  @MockBean private AcceptableEventFilter acceptableEventFilter;

  @BeforeEach
  public void initMocks() {
    Mockito.reset(receiver);
  }

  /** Test the receiver flow */
  @Test
  public void caseEventFlowTest() throws Exception {
    Header header = new Header();
    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    caseEvent.setHeader(header);

    // Construct message
    Message<CaseEvent> message = new GenericMessage<>(caseEvent, new HashMap<>());

    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    surveyUpdate.setSampleDefinitionUrl("test/social.json");
    when(respondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));
    CollectionExercise collectionExercise = new CollectionExercise();
    when(respondentDataRepo.readCollectionExercise(any()))
        .thenReturn(Optional.of(collectionExercise));

    // Send message to container
    caseEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    verify(receiver).acceptCaseEvent(captur.capture());
    assertEquals(captur.getValue().getPayload(), caseEvent.getPayload());
    verify(respondentDataRepo, times(1)).writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
  }

  /** Test the receiver flow for SIS case event */
  @Test
  public void caseEventSISFlowTest() throws Exception {
    Header header = new Header();
    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    caseEvent.setHeader(header);

    // Construct message
    Message<CaseEvent> message = new GenericMessage<>(caseEvent, new HashMap<>());

    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    surveyUpdate.setSampleDefinitionUrl("test/socialnot.json");
    when(respondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));

    // Send message to container
    caseEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    verify(receiver).acceptCaseEvent(captur.capture());
    assertEquals(captur.getValue().getPayload(), caseEvent.getPayload());
    verify(respondentDataRepo, times(0)).writeCaseUpdate(caseEvent.getPayload().getCaseUpdate());
  }

  @Test
  public void caseCaseReceivedWithoutMillisTest() throws Exception {

    // Create a case with a timestamp. Note that the milliseconds are not specified
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);

    Header header = new Header();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    header.setDateTime(sdf.parse("2011-08-12T20:17:46Z"));
    header.setMessageId(UUID.fromString("c45de4dc-3c3b-11e9-b210-d663bd873d93"));
    caseEvent.setHeader(header);

    // Construct message
    Message<CaseEvent> message = new GenericMessage<>(caseEvent, new HashMap<>());

    SurveyUpdate surveyUpdate = new SurveyUpdate();
    surveyUpdate.setSurveyId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    surveyUpdate.setSampleDefinitionUrl("test/social.json");
    when(respondentDataRepo.readSurvey(any())).thenReturn(Optional.of(surveyUpdate));

    // Send message to container
    caseEventInbound.getOutputChannel().send(message);

    // Capture and check Service Activator argument
    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    verify(receiver).acceptCaseEvent(captur.capture());
    assertEquals(sdf.parse("2011-08-12T20:17:46Z"), captur.getValue().getHeader().getDateTime());
    assertEquals(caseEvent.getHeader(), captur.getValue().getHeader());
    assertEquals(caseEvent.getPayload(), captur.getValue().getPayload());
  }
}
