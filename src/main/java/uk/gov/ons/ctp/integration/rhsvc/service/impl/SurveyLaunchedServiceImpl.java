package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchResponse;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.SurveyLaunchedService;

import java.util.UUID;

/** This is a service layer class, which performs RH business level logic for the endpoints. */
@Slf4j
@Service
public class SurveyLaunchedServiceImpl implements SurveyLaunchedService {
  private EventPublisher eventPublisher;
  private RateLimiterClient rateLimiterClient;
  private AppConfig appConfig;

  @Autowired
  public SurveyLaunchedServiceImpl(
      EventPublisher eventPublisher, RateLimiterClient rateLimiterClient, AppConfig appConfig) {
    this.eventPublisher = eventPublisher;
    this.rateLimiterClient = rateLimiterClient;
    this.appConfig = appConfig;
  }

  @Override
  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) throws CTPException {
    log.info("Generating SurveyLaunched event", kv("surveyLaunchedDTO", surveyLaunchedDTO));

    checkRateLimit(surveyLaunchedDTO.getClientIP());

    SurveyLaunchResponse response =
        SurveyLaunchResponse.builder()
            .questionnaireId(surveyLaunchedDTO.getQuestionnaireId())
            .caseId(surveyLaunchedDTO.getCaseId())
            .agentId(surveyLaunchedDTO.getAgentId())
            .build();

    Channel channel = Channel.RH;
    if (!StringUtils.isEmpty(surveyLaunchedDTO.getAgentId())) {
      channel = Channel.AD;
    }

    UUID transactionId =
        eventPublisher.sendEvent(
            TopicType.SURVEY_LAUNCH, Source.RESPONDENT_HOME, channel, response);

    log.debug(
        "SurveyLaunch event published",
        kv("caseId", response.getCaseId()),
        kv("transactionId", transactionId.toString()));
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
