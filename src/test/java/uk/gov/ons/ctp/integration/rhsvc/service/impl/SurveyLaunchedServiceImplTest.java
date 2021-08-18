package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.LoadsheddingConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.RateLimiterConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;

@ExtendWith(MockitoExtension.class)
public class SurveyLaunchedServiceImplTest {
  private static final int A_MODULUS = 10;
  private static final String AN_IP_ADDR = "254.123.786.3";

  @Mock private EventPublisher publisher;
  @Mock private RateLimiterClient rateLimiterClient;
  @Mock private AppConfig appConfig;

  @InjectMocks SurveyLaunchedServiceImpl surveyLaunchedService;

  @Captor ArgumentCaptor<SurveyLaunchResponse> sendEventCaptor;

  private SurveyLaunchedDTO surveyLaunchedDTO;

  @BeforeEach
  public void setup() {
    mockEnableRateLimiter(true);

    LoadsheddingConfig loadsheddingConf = new LoadsheddingConfig();
    loadsheddingConf.setModulus(10);
    lenient().when(appConfig.getLoadshedding()).thenReturn(loadsheddingConf);

    createPayload();
  }

  private void createPayload() {
    surveyLaunchedDTO = new SurveyLaunchedDTO();
    surveyLaunchedDTO.setQuestionnaireId("1234");
    surveyLaunchedDTO.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    surveyLaunchedDTO.setAgentId("1000007");
    surveyLaunchedDTO.setClientIP(AN_IP_ADDR);
  }

  private void mockEnableRateLimiter(boolean enabled) {
    RateLimiterConfig config = new RateLimiterConfig();
    config.setEnabled(enabled);
    when(appConfig.getRateLimiter()).thenReturn(config);
  }

  private void verifyRateLimiterCalled() throws Exception {
    verify(rateLimiterClient).checkEqLaunchLimit(eq(Domain.RH), eq(AN_IP_ADDR), eq(A_MODULUS));
  }

  private void verifyRateLimiterNotCalled() throws Exception {
    verify(rateLimiterClient, never())
        .checkEqLaunchLimit(eq(Domain.RH), eq(AN_IP_ADDR), eq(A_MODULUS));
  }

  private void callAndVerifySurveyLaunched(Channel expectedChannel) throws Exception {
    surveyLaunchedService.surveyLaunched(surveyLaunchedDTO);

    // Get hold of the event pay load that surveyLaunchedService created
    verify(publisher)
        .sendEvent(
            eq(EventType.SURVEY_LAUNCH),
            eq(Source.RESPONDENT_HOME),
            eq(expectedChannel),
            sendEventCaptor.capture());
    SurveyLaunchResponse eventPayload = sendEventCaptor.getValue();

    // Verify contents of pay load object
    assertEquals(surveyLaunchedDTO.getQuestionnaireId(), eventPayload.getQuestionnaireId());
    assertEquals(surveyLaunchedDTO.getCaseId(), eventPayload.getCaseId());
    assertEquals(surveyLaunchedDTO.getAgentId(), eventPayload.getAgentId());
  }

  @Test
  public void testSurveyLaunchedAddressAgentIdValue() throws Exception {
    callAndVerifySurveyLaunched(Channel.AD);
    verifyRateLimiterCalled();
  }

  @Test
  public void testSurveyLaunchedAddressAgentIdEmptyString() throws Exception {
    surveyLaunchedDTO.setAgentId("");
    callAndVerifySurveyLaunched(Channel.RH);
    verifyRateLimiterCalled();
  }

  @Test
  public void testSurveyLaunchedAddressAgentIdNull() throws Exception {
    surveyLaunchedDTO.setAgentId(null);
    callAndVerifySurveyLaunched(Channel.RH);
    verifyRateLimiterCalled();
  }

  @Test
  public void shouldNotCallRateLimterWhenNotEnabled() throws Exception {
    mockEnableRateLimiter(false);
    callAndVerifySurveyLaunched(Channel.AD);
    verifyRateLimiterNotCalled();
  }
}
