package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.EqLaunchResponse;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.EqLaunchedService;

/** This is a service layer class, which performs RH business level logic for the endpoints. */
@Slf4j
@Service
public class EqLaunchedServiceImpl implements EqLaunchedService {
  private EventPublisher eventPublisher;
  private RateLimiterClient rateLimiterClient;
  private AppConfig appConfig;

  @Autowired
  public EqLaunchedServiceImpl(
      EventPublisher eventPublisher, RateLimiterClient rateLimiterClient, AppConfig appConfig) {
    this.eventPublisher = eventPublisher;
    this.rateLimiterClient = rateLimiterClient;
    this.appConfig = appConfig;
  }

  @Override
  public void eqLaunched(EqLaunchedDTO eqLaunchedDTO) throws CTPException {
    log.info("Generating EqLaunched event", kv("eqLaunchedDTO", eqLaunchedDTO));

    checkRateLimit(eqLaunchedDTO.getClientIP());

    EqLaunchResponse response =
        EqLaunchResponse.builder()
            .questionnaireId(eqLaunchedDTO.getQuestionnaireId())
            .caseId(eqLaunchedDTO.getCaseId())
            .agentId(eqLaunchedDTO.getAgentId())
            .build();

    Channel channel = Channel.RH;
    if (!StringUtils.isEmpty(eqLaunchedDTO.getAgentId())) {
      channel = Channel.AD;
    }

    UUID messageId =
        eventPublisher.sendEvent(TopicType.EQ_LAUNCH, Source.RESPONDENT_HOME, channel, response);

    log.debug(
        "EqLaunch event published", kv("caseId", response.getCaseId()), kv("messageId", messageId));
  }

  private void checkRateLimit(String ipAddress) throws CTPException {
    if (appConfig.getRateLimiter().isEnabled()) {
      int modulus = appConfig.getLoadshedding().getModulus();
      log.debug(
          "Invoking rate limiter for survey launched",
          kv("ipAddress", ipAddress),
          kv("loadshedding.modulus", modulus));
      rateLimiterClient.checkEqLaunchLimit(Domain.RH, ipAddress, modulus);
    } else {
      log.info("Rate limiter client is disabled");
    }
  }
}
