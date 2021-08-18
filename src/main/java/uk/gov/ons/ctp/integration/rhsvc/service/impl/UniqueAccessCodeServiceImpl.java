package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.FormType;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UacAuthenticateResponse;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;
import uk.gov.ons.ctp.integration.rhsvc.service.UniqueAccessCodeService;

/** Implementation to deal with UAC data */
@Slf4j
@Service
public class UniqueAccessCodeServiceImpl implements UniqueAccessCodeService {
  @Autowired private AppConfig appConfig;
  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private MapperFacade mapperFacade;

  // Enums to capture the linking matrix of valid form type and case types.
  // Original table is from:
  // https://collaborate2.ons.gov.uk/confluence/display/SDC/RH+-+Authentication+-+Unlinked+UAC
  // https://collaborate2.ons.gov.uk/confluence/display/SDC/Business+Rules
  private enum LinkingCombination {
    H1(FormType.H, CaseType.HH),
    H2(FormType.H, CaseType.SPG),
    I1(FormType.I, CaseType.HH),
    I2(FormType.I, CaseType.SPG),
    I3(FormType.I, CaseType.CE),
    C1(FormType.C, CaseType.CE);

    private FormType uacFormType;
    private CaseType caseCaseType;

    private LinkingCombination(FormType uacFormType, CaseType caseCaseType) {
      this.uacFormType = uacFormType;
      this.caseCaseType = caseCaseType;
    }

    static Optional<LinkingCombination> lookup(FormType uacFormType, CaseType caseCaseType) {
      return Stream.of(LinkingCombination.values())
          .filter(row -> row.uacFormType == uacFormType && row.caseCaseType == caseCaseType)
          .findAny();
    }
  }

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
        Optional<CollectionCase> caseMatch = dataRepo.readCollectionCase(caseId);
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
      sendRespondentAuthenticatedEvent(data);
    } else {
      log.warn("Unknown UAC", kv("uacHash", uacHash));
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UAC");
    }

    return data;
  }

  /** Send RespondentAuthenticated event */
  private void sendRespondentAuthenticatedEvent(UniqueAccessCodeDTO data) throws CTPException {

    log.info(
        "Generating RespondentAuthenticated event for caseId",
        kv("caseId", data.getCaseId()),
        kv("questionnaireId", data.getQuestionnaireId()));

    UacAuthenticateResponse response =
        UacAuthenticateResponse.builder()
            .questionnaireId(data.getQuestionnaireId())
            .caseId(data.getCaseId())
            .build();

    String transactionId =
        eventPublisher.sendEvent(
            EventType.UAC_AUTHENTICATE, Source.RESPONDENT_HOME, Channel.RH, response);

    log.debug(
        "RespondentAuthenticated event published for caseId: "
            + response.getCaseId()
            + ", transactionId: "
            + transactionId);
  }

  private UniqueAccessCodeDTO createUniqueAccessCodeDTO(
      UAC uac, Optional<CollectionCase> collectionCase, CaseStatus caseStatus) {
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
