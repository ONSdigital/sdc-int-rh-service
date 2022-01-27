package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.EqLaunch;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacAuthentication;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.CaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.UacRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CollectionExerciseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.LaunchDataDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACContextDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLiteDTO;

/** Implementation to deal with UAC data */
@Slf4j
@Service
public class UniqueAccessCodeServiceImpl {
  @Autowired private SurveyRepository surveyDataRepo;
  @Autowired private CollectionExerciseRepository collExDataRepo;
  @Autowired private CaseRepository caseDataRepo;
  @Autowired private UacRepository uacDataRepo;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private MapperFacade mapperFacade;

  @Autowired private EqLaunchServiceImpl eqLaunchedService;
  @Autowired private RateLimiterClient rateLimiterClient;
  @Autowired private AppConfig appConfig;

  /** Constructor */
  public UniqueAccessCodeServiceImpl() {}

  /**
   * Retrieve the data for a hashed UAC, and send an authentication event.
   *
   * @param uacHash hashed unique access code for which to retrieve data.
   * @return RhClaimsResponseDTO holding the UAC data to respond to the request.
   * @throws CTPException something went wrong
   */
  public UACContextDTO getUACClaimContext(String uacHash) throws CTPException {

    LaunchDataDTO launchData = gatherLaunchData(uacHash);

    sendUacAuthenticationEvent(
        launchData.getCaseUpdate().getCaseId(), launchData.getUacUpdate().getQid());

    UACContextDTO uacContextDTO =
        createRhClaimsResponseDTO(
            launchData.getUacUpdate(),
            launchData.getCaseUpdate(),
            launchData.getCollectionExerciseUpdate(),
            launchData.getSurveyUpdate());

    return uacContextDTO;
  }

  /**
   * Creates the EQ launch URL, and also sends UAC authentication and launch events.
   *
   * @param uacHash uacHash hashed unique access code for which to retrieve data.
   * @param eqLaunchedDTO contains data supplied to the endpoint which is needed in order to be able
   *     to create the EQ launch URL.
   * @return String containing the EQ launch URL.
   * @throws CTPException if something went wrong.
   */
  public String generateEqLaunchToken(String uacHash, EqLaunchRequestDTO eqLaunchedDTO)
      throws CTPException {

    log.info(
        "Generating eq launch url and publish Launched event", kv("eqLaunchedDTO", eqLaunchedDTO));

    checkRateLimit(eqLaunchedDTO.getClientIP());

    // Build launch URL
    LaunchDataDTO launchData = gatherLaunchData(uacHash);
    String eqLaunchUrl = eqLaunchedService.createLaunchUrl(launchData, eqLaunchedDTO);

    // Publish the launch event
    EqLaunch eqLaunch = EqLaunch.builder().qid(launchData.getUacUpdate().getQid()).build();
    UUID messageId =
        eventPublisher.sendEvent(TopicType.EQ_LAUNCH, Source.RESPONDENT_HOME, Channel.RH, eqLaunch);
    log.debug(
        "EqLaunch event published",
        kv("qid", eqLaunch.getQid()),
        kv("messageId", messageId),
        kv("caseId", launchData.getCaseUpdate().getCaseId()));

    return eqLaunchUrl;
  }

  private LaunchDataDTO gatherLaunchData(String uacHash) throws CTPException {

    LaunchDataDTO launchData;

    UacUpdate uac =
        uacDataRepo
            .readUAC(uacHash)
            .orElseThrow(
                () ->
                    new CTPException(
                        CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC"));

    String caseId = uac.getCaseId();
    if (StringUtils.isEmpty(caseId)) {
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "UAC has no caseId");
    }

    CaseUpdate caseUpdate =
        caseDataRepo
            .readCaseUpdate(caseId)
            .orElseThrow(() -> new CTPException(CTPException.Fault.SYSTEM_ERROR, "Case Not Found"));
    SurveyUpdate survey =
        surveyDataRepo
            .readSurvey(caseUpdate.getSurveyId())
            .orElseThrow(
                () -> new CTPException(CTPException.Fault.SYSTEM_ERROR, "Survey Not Found"));
    CollectionExerciseUpdate collex =
        collExDataRepo
            .readCollectionExercise(caseUpdate.getCollectionExerciseId())
            .orElseThrow(
                () ->
                    new CTPException(
                        CTPException.Fault.SYSTEM_ERROR, "CollectionExercise Not Found"));

    launchData =
        LaunchDataDTO.builder()
            .uacUpdate(uac)
            .caseUpdate(caseUpdate)
            .collectionExerciseUpdate(collex)
            .surveyUpdate(survey)
            .build();

    return launchData;
  }

  /** Send UacAuthentication event */
  private void sendUacAuthenticationEvent(String caseId, String qid) throws CTPException {

    log.info(
        "Generating UacAuthentication event for caseId",
        kv("caseId", caseId),
        kv("questionnaireId", qid));

    UacAuthentication uacAuthentication = UacAuthentication.builder().qid(qid).build();

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.UAC_AUTHENTICATION, Source.RESPONDENT_HOME, Channel.RH, uacAuthentication);

    log.debug(
        "UacAuthentication event published for qid: "
            + uacAuthentication.getQid()
            + ", messageId: "
            + messageId);
  }

  private UACContextDTO createRhClaimsResponseDTO(
      UacUpdate uac,
      CaseUpdate collectionCase,
      CollectionExerciseUpdate collectionExercise,
      SurveyUpdate surveyUpdate) {
    UACContextDTO uniqueAccessCodeDTO = new UACContextDTO();

    mapperFacade.map(uac, uniqueAccessCodeDTO);

    CaseDTO caseDTO = new CaseDTO();
    mapperFacade.map(collectionCase, caseDTO);
    uniqueAccessCodeDTO.setCollectionCase(caseDTO);

    CollectionExerciseDTO collectionExerciseDTO = new CollectionExerciseDTO();
    mapperFacade.map(collectionExercise, collectionExerciseDTO);
    uniqueAccessCodeDTO.setCollectionExercise(collectionExerciseDTO);

    SurveyLiteDTO surveyLiteDTO = new SurveyLiteDTO();
    mapperFacade.map(surveyUpdate, surveyLiteDTO);
    uniqueAccessCodeDTO.setSurvey(surveyLiteDTO);

    return uniqueAccessCodeDTO;
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
