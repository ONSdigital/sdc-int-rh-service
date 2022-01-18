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
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.EqLaunch;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.LoadsheddingConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.RateLimiterConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchDTO;

@ExtendWith(MockitoExtension.class)
public class EqLaunchedServiceImplTest {
  private static final int A_MODULUS = 10;
  private static final String AN_IP_ADDR = "254.123.786.3";

  @Mock private EventPublisher publisher;
  @Mock private RateLimiterClient rateLimiterClient;
  @Mock private AppConfig appConfig;

  @InjectMocks EqLaunchedServiceImpl eqLaunchedService;

  @Captor ArgumentCaptor<EqLaunch> sendEventCaptor;

  private EqLaunchDTO eqLaunchedDTO;

  @BeforeEach
  public void setup() {
    mockEnableRateLimiter(true);

    LoadsheddingConfig loadsheddingConf = new LoadsheddingConfig();
    loadsheddingConf.setModulus(10);
    lenient().when(appConfig.getLoadshedding()).thenReturn(loadsheddingConf);

    createPayload();
  }

  private void createPayload() {
    eqLaunchedDTO = new EqLaunchDTO();
    eqLaunchedDTO.setQuestionnaireId("1234");
    eqLaunchedDTO.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    eqLaunchedDTO.setAgentId("1000007");
    eqLaunchedDTO.setClientIP(AN_IP_ADDR);
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

  private void callAndVerifyEqLaunched(Channel expectedChannel) throws Exception {
    eqLaunchedService.eqLaunched(eqLaunchedDTO);

    // Get hold of the event pay load that eqLaunchedService created
    verify(publisher)
        .sendEvent(
            eq(TopicType.EQ_LAUNCH),
            eq(Source.RESPONDENT_HOME),
            eq(expectedChannel),
            sendEventCaptor.capture());
    EqLaunch eventPayload = sendEventCaptor.getValue();

    // Verify contents of pay load object
    assertEquals(eqLaunchedDTO.getQuestionnaireId(), eventPayload.getQid());
  }

  @Test
  public void testEqLaunchedAddressAgentIdValue() throws Exception {
    callAndVerifyEqLaunched(Channel.AD);
    verifyRateLimiterCalled();
  }

  @Test
  public void testEqLaunchedAddressAgentIdEmptyString() throws Exception {
    eqLaunchedDTO.setAgentId("");
    callAndVerifyEqLaunched(Channel.RH);
    verifyRateLimiterCalled();
  }

  @Test
  public void testEqLaunchedAddressAgentIdNull() throws Exception {
    eqLaunchedDTO.setAgentId(null);
    callAndVerifyEqLaunched(Channel.RH);
    verifyRateLimiterCalled();
  }

  @Test
  public void shouldNotCallRateLimterWhenNotEnabled() throws Exception {
    mockEnableRateLimiter(false);
    callAndVerifyEqLaunched(Channel.AD);
    verifyRateLimiterNotCalled();
  }
}
