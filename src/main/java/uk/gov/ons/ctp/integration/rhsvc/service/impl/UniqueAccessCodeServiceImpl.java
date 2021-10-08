package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Optional;
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
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;
import uk.gov.ons.ctp.integration.rhsvc.service.UniqueAccessCodeService;

/** Implementation to deal with UAC data */
@Slf4j
@Service
public class UniqueAccessCodeServiceImpl implements UniqueAccessCodeService {
  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private MapperFacade mapperFacade;

  /** Constructor */
  public UniqueAccessCodeServiceImpl() {}

  @Override
  public UniqueAccessCodeDTO getAndAuthenticateUAC(String uacHash) throws CTPException {

    UniqueAccessCodeDTO data;
    Optional<UAC> uacMatch = dataRepo.readUAC(uacHash);
    if (uacMatch.isPresent()) {
      // we found UAC
      String caseId = uacMatch.get().getCaseId();
      if (!StringUtils.isEmpty(caseId)) {
        // UAC has a caseId
        Optional<CaseUpdate> caseMatch = dataRepo.readCaseUpdate(caseId);
        if (caseMatch.isPresent()) {
          // Case found
          log.debug("UAC is linked", kv("uacHash", uacHash), kv("caseId", caseId));
          data = createUniqueAccessCodeDTO(uacMatch.get(), caseMatch, CaseStatus.OK);
        } else {
          // Case NOT found
          log.info(
              "Cannot find Case for UAC - telling UI unlinked - RM remediation required",
              kv("uacHash", uacHash),
              kv("caseId", caseId));
          data = createUniqueAccessCodeDTO(uacMatch.get(), Optional.empty(), CaseStatus.UNLINKED);
          data.setCaseId(null);
        }
      } else {
        // unlinked logkv(uacHash)
        log.debug("UAC is unlinked", kv("uacHash", uacHash));
        data = createUniqueAccessCodeDTO(uacMatch.get(), Optional.empty(), CaseStatus.UNLINKED);
      }
      sendUacAuthenticatedEvent(data);
    } else {
      log.warn("Unknown UAC", kv("uacHash", uacHash));
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC");
    }

    return data;
  }

  /** Send UacAuthenticated event */
  private void sendUacAuthenticatedEvent(UniqueAccessCodeDTO data) throws CTPException {

    log.info(
        "Generating UacAuthenticated event for caseId",
        kv("caseId", data.getCaseId()),
        kv("questionnaireId", data.getQuestionnaireId()));

    UacAuthenticateResponse response =
        UacAuthenticateResponse.builder()
            .questionnaireId(data.getQuestionnaireId())
            .caseId(data.getCaseId())
            .build();

    UUID messageId =
        eventPublisher.sendEvent(
            TopicType.UAC_AUTHENTICATE, Source.RESPONDENT_HOME, Channel.RH, response);

    log.debug(
        "UacAuthenticated event published for caseId: "
            + response.getCaseId()
            + ", messageId: "
            + messageId);
  }

  private UniqueAccessCodeDTO createUniqueAccessCodeDTO(
      UAC uac, Optional<CaseUpdate> collectionCase, CaseStatus caseStatus) {
    UniqueAccessCodeDTO uniqueAccessCodeDTO = new UniqueAccessCodeDTO();

    // Copy the UAC first, then Case

    mapperFacade.map(uac, uniqueAccessCodeDTO);

    if (collectionCase.isPresent()) {
      mapperFacade.map(collectionCase.get(), uniqueAccessCodeDTO);
    }

    uniqueAccessCodeDTO.setCaseStatus(caseStatus);

    return uniqueAccessCodeDTO;
  }
}
