package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacAuthentication;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.repository.CaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.CollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.SurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.UacRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CollectionExerciseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.RhClaimsDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLiteDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.ClaimsDataDTO;

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
  
  @Autowired private EqLaunchedServiceImpl eqLaunchedService;


  /** Constructor */
  public UniqueAccessCodeServiceImpl() {}

  public RhClaimsDTO getUACClaimContext(String uacHash) throws CTPException {
    
    ClaimsDataDTO claims = buildClaimsData(uacHash);
    
    sendUacAuthenticationEvent(claims.getCaseUpdate().getCaseId(), claims.getUacUpdate().getQid());
    
    RhClaimsDTO rhClaimsDTO = createRhClaimsDTO(claims.getUacUpdate(), claims.getCaseUpdate(), claims.getCollectionExerciseUpdate(), claims.getSurveyUpdate());
    
    return rhClaimsDTO;
  }
  
  public String createEqLaunchUrl(String uacHash, EqLaunchDTO eqLaunchedDTO) throws CTPException {

    ClaimsDataDTO claims = buildClaimsData(uacHash);
    
    sendUacAuthenticationEvent(claims.getCaseUpdate().getCaseId(), claims.getUacUpdate().getQid());
    
    ClaimsDataDTO uacDTO = createUniqueAccessCode2DTO(claims.getUacUpdate(), claims.getCaseUpdate(), claims.getCollectionExerciseUpdate(), claims.getSurveyUpdate());
    String launchURL = eqLaunchedService.eqLaunched(uacDTO, eqLaunchedDTO);

    return launchURL;
  }
  
  private ClaimsDataDTO buildClaimsData(String uacHash) throws CTPException {

    ClaimsDataDTO data;
    UacUpdate uac =
        uacDataRepo
            .readUAC(uacHash)
            .orElseThrow(
                () ->
                    new CTPException(
                        CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC"));
    String caseId = uac.getCaseId();
    if (!StringUtils.isEmpty(caseId)) {
      // UAC has a caseId
      CaseUpdate caseUpdate =
          caseDataRepo
              .readCaseUpdate(caseId)
              .orElseThrow(
                  () -> new CTPException(CTPException.Fault.SYSTEM_ERROR, "Case Not Found"));
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

      data = createUniqueAccessCode2DTO(uac, caseUpdate, collex, survey);
    } else {
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "UAC has no caseId");
    }

    return data;
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

  private ClaimsDataDTO createUniqueAccessCode2DTO(
      UacUpdate uac,
      CaseUpdate collectionCase,
      CollectionExerciseUpdate collectionExercise,
      SurveyUpdate surveyUpdate) {
    ClaimsDataDTO uniqueAccessCode2DTO = new ClaimsDataDTO();

    uniqueAccessCode2DTO.setUacUpdate(uac);
    uniqueAccessCode2DTO.setCaseUpdate(collectionCase);
    uniqueAccessCode2DTO.setCollectionExerciseUpdate(collectionExercise);
    uniqueAccessCode2DTO.setSurveyUpdate(surveyUpdate);

    return uniqueAccessCode2DTO;
  }
  
  private RhClaimsDTO createRhClaimsDTO(
      UacUpdate uac,
      CaseUpdate collectionCase,
      CollectionExerciseUpdate collectionExercise,
      SurveyUpdate surveyUpdate) {
    RhClaimsDTO uniqueAccessCodeDTO = new RhClaimsDTO();

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
}
