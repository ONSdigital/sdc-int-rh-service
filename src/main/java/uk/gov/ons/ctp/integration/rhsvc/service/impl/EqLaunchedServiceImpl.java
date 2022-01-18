package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.EqLaunch;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.ClaimsDataDTO;

/** This is a service layer class, which performs RH business level logic for the endpoints. */
@Slf4j
@Service
public class EqLaunchedServiceImpl {
  private EventPublisher eventPublisher;
  private RateLimiterClient rateLimiterClient;
  private AppConfig appConfig;
  
  @Autowired private EqLaunchService eqLaunchService;

  @Autowired
  public EqLaunchedServiceImpl(
      EventPublisher eventPublisher, RateLimiterClient rateLimiterClient, AppConfig appConfig) {
    this.eventPublisher = eventPublisher;
    this.rateLimiterClient = rateLimiterClient;
    this.appConfig = appConfig;
  }

  public String generateEqLaunchToken(ClaimsDataDTO claimsDataDTO, EqLaunchDTO eqLaunchedDTO) throws CTPException {
    log.info("Generating EqLaunched event and url", kv("eqLaunchedDTO", eqLaunchedDTO));

    checkRateLimit(eqLaunchedDTO.getClientIP());

    EqLaunch eqLaunch = EqLaunch.builder().qid(claimsDataDTO.getUacUpdate().getQid()).build();

    UUID messageId =
        eventPublisher.sendEvent(TopicType.EQ_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, eqLaunch);

    log.debug("EqLaunch event published", kv("qid", eqLaunch.getQid()), kv("messageId", messageId));
    
    String eqUrl = createLaunchUrl(claimsDataDTO, eqLaunchedDTO);
    
    return eqUrl;
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
  
  private String createLaunchUrl(ClaimsDataDTO uac2DTO, EqLaunchDTO eqLaunchedDTO)
      throws CTPException {

    String encryptedPayload = "";

    UacUpdate uacUpdate = uac2DTO.getUacUpdate();
    CaseUpdate caze = uac2DTO.getCaseUpdate();
    CollectionExerciseUpdate collex = uac2DTO.getCollectionExerciseUpdate();
    SurveyUpdate survey = uac2DTO.getSurveyUpdate();

    try {
      EqLaunchData eqLaunchData =
          EqLaunchData.builder()
              .language(eqLaunchedDTO.getLanguageCode())
              .source(Source.CONTACT_CENTRE_API)
              .channel(Channel.CC)
              .salt(appConfig.getEq().getResponseIdSalt())
              .surveyType(SurveyType.fromSampleDefinitionUrl(survey.getSampleDefinitionUrl()))
              .collectionExerciseUpdate(collex)
              .uacUpdate(uacUpdate)
              .caseUpdate(caze)
              .userId("RH")
              .accountServiceUrl(eqLaunchedDTO.getAccountServiceUrl())
              .accountServiceLogoutUrl(eqLaunchedDTO.getAccountServiceLogoutUrl())
              .build();
      encryptedPayload = eqLaunchService.getEqLaunchJwe(eqLaunchData);

    } catch (CTPException e) {
      log.error(
          "Failed to create JWE payload for eq launch",
          kv("caseId", caze.getCaseId()),
          kv("questionnaireId", uacUpdate.getQid()),
          e);
      throw e;
    }
    String eqUrl =
        appConfig.getEq().getProtocol()
            + "://"
            + appConfig.getEq().getHost()
            + appConfig.getEq().getPath()
            + encryptedPayload;
    if (log.isDebugEnabled()) {
      log.debug("Have created launch URL", kv("launchURL", eqUrl));
    }
    return eqUrl;
  }
}
