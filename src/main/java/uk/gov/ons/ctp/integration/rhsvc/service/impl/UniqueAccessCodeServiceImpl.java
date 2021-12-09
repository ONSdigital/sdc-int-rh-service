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
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacAuthentication;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentCaseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentCollectionExerciseRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentSurveyRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentUacRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CollectionExerciseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLiteDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.UniqueAccessCodeService;

/** Implementation to deal with UAC data */
@Slf4j
@Service
public class UniqueAccessCodeServiceImpl implements UniqueAccessCodeService {
  @Autowired private RespondentSurveyRepository surveyDataRepo;
  @Autowired private RespondentCollectionExerciseRepository collExDataRepo;
  @Autowired private RespondentCaseRepository caseDataRepo;
  @Autowired private RespondentUacRepository uacDataRepo;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private MapperFacade mapperFacade;

  /** Constructor */
  public UniqueAccessCodeServiceImpl() {}

  @Override
  public UniqueAccessCodeDTO getAndAuthenticateUAC(String uacHash) throws CTPException {

    UniqueAccessCodeDTO data;
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
      CollectionExercise collex =
          collExDataRepo
              .readCollectionExercise(caseUpdate.getCollectionExerciseId())
              .orElseThrow(
                  () ->
                      new CTPException(
                          CTPException.Fault.SYSTEM_ERROR, "CollectionExercise Not Found"));

      data = createUniqueAccessCodeDTO(uac, caseUpdate, collex, survey);
      sendUacAuthenticationEvent(data);
    } else {
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "UAC has no caseId");
    }

    return data;
  }

  /** Send UacAuthentication event */
  private void sendUacAuthenticationEvent(UniqueAccessCodeDTO data) throws CTPException {
    UUID caseId = data.getCollectionCase().getCaseId();

    log.info(
        "Generating UacAuthentication event for caseId",
        kv("caseId", caseId),
        kv("questionnaireId", data.getQid()));

    UacAuthentication uacAuthentication = UacAuthentication.builder().qid(data.getQid()).build();

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.UAC_AUTHENTICATION, Source.RESPONDENT_HOME, Channel.RH, uacAuthentication);

    log.debug(
        "UacAuthentication event published for qid: "
            + uacAuthentication.getQid()
            + ", messageId: "
            + messageId);
  }

  private UniqueAccessCodeDTO createUniqueAccessCodeDTO(
      UacUpdate uac,
      CaseUpdate collectionCase,
      CollectionExercise collectionExercise,
      SurveyUpdate surveyUpdate) {
    UniqueAccessCodeDTO uniqueAccessCodeDTO = new UniqueAccessCodeDTO();

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
