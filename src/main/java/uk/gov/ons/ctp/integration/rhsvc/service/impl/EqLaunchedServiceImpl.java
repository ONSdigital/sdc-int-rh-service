package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.ClaimsDataDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchDTO;

/** This is a service layer class, which performs RH business level logic for the endpoints. */
@Slf4j
@Service
public class EqLaunchedServiceImpl {
  @Autowired private AppConfig appConfig;
  @Autowired private EqLaunchService eqLaunchService;

  String createLaunchUrl(ClaimsDataDTO uac2DTO, EqLaunchDTO eqLaunchedDTO)
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
