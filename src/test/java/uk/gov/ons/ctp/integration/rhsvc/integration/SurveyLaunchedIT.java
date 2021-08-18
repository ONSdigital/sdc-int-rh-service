package uk.gov.ons.ctp.integration.rhsvc.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.TestHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.PubSubEventSender;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.rhsvc.endpoint.SurveyLaunchedEndpoint;

/**
 * This is a component test which submits a Post saying that a survey has been launched and uses a
 * mock of RabbitMQ to confirm that RH publishes a survey launched event.
 */
@Disabled
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
public class SurveyLaunchedIT {
  @Autowired private SurveyLaunchedEndpoint surveyLaunchedEndpoint;

  @MockBean private PubSubTemplate pubSubTemplate;

  @Autowired EventPublisher eventPublisher;

  private MockMvc mockMvc;

  @Captor private ArgumentCaptor<SurveyLaunchEvent> publishCaptor;

  @BeforeEach
  public void setUp() throws Exception {
    EventSender sender = new PubSubEventSender(pubSubTemplate, 60);
    ReflectionTestUtils.setField(eventPublisher, "sender", sender);

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(surveyLaunchedEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .build();
  }

  /**
   * This test Posts a survey launched event and uses a mock rabbit template to confirm that a
   * survey launched event is published.
   */
  @Test
  public void surveyLaunched_success() throws Exception {
    // Read request body from resource file
    ObjectNode surveyLaunchedRequestBody = FixtureHelper.loadClassObjectNode();
    String questionnaireId = surveyLaunchedRequestBody.get("questionnaireId").asText();
    String caseIdString = surveyLaunchedRequestBody.get("caseId").asText();

    // Send a Post request to the /surveyLaunched endpoint
    System.out.println(surveyLaunchedRequestBody.toString());
    mockMvc
        .perform(postJson("/surveyLaunched", surveyLaunchedRequestBody.toString()))
        .andExpect(status().isOk());

    // Get ready to capture the survey details published to the exchange
    Mockito.verify(pubSubTemplate).publish(eq("event_survey-launch"), publishCaptor.capture());
    SurveyLaunchEvent publishedEvent = publishCaptor.getValue();

    // Validate contents of the published event
    Header event = publishedEvent.getEvent();
    assertEquals("SURVEY_LAUNCHED", event.getType());
    assertEquals("RESPONDENT_HOME", event.getSource());
    assertEquals("RH", event.getChannel());
    assertNotNull(event.getDateTime());
    TestHelper.validateAsUUID(event.getTransactionId());
    // Verify content of 'payload' part
    SurveyLaunchResponse response = publishedEvent.getPayload().getResponse();
    assertEquals(questionnaireId, response.getQuestionnaireId());
    assertEquals(UUID.fromString(caseIdString), response.getCaseId());
    assertNull(response.getAgentId());
  }

  //  /**
  //   * This simulates a Rabbit failure during event posting, which should result in a 500
  // (internal
  //   * server) error.
  //   */
  //  @Test
  //  public void surveyLaunched_failsOnSend() throws Exception {
  //    // Simulate event posting failure
  //    Mockito.doThrow(AmqpException.class)
  //        .when(rabbitTemplate)
  //        .convertAndSend((String) eq("event.response.authentication"), (Object) any());
  //
  //    // Read request body from resource file
  //    ObjectNode surveyLaunchedRequestBody = FixtureHelper.loadClassObjectNode();
  //
  //    // Send a Post request to the /surveyLaunched endpoint
  //    mockMvc
  //        .perform(postJson("/surveyLaunched", surveyLaunchedRequestBody.toString()))
  //        .andExpect(status().isInternalServerError());
  //  }
}
