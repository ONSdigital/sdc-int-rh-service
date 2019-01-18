package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseDetailsService;

// import java.util.stream.Collectors;
// import java.util.Objects;
// import org.springframework.data.domain.Example;
// import org.springframework.data.domain.ExampleMatcher;
// import uk.gov.ons.ctp.integration.casesvc.representation.CaseDetailsDTO;
// import uk.gov.ons.ctp.integration.casesvc.representation.CaseEventCreationRequestDTO;
// import uk.gov.ons.ctp.integration.casesvc.representation.CaseEventDTO;
// import uk.gov.ons.ctp.integration.casesvc.representation.CaseGroupDTO;
// import uk.gov.ons.ctp.integration.casesvc.representation.CategoryDTO;
// import uk.gov.ons.ctp.integration.casesvc.representation.CreatedCaseEventDTO;
// import uk.gov.ons.ctp.integration.rhsvc.domain.model.Case;
// import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;
// import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseGroup;
// import uk.gov.ons.ctp.integration.rhsvc.domain.model.Category;
// import uk.gov.ons.ctp.integration.rhsvc.domain.repository.CaseRepository;

/** The REST endpoint controller for CaseSvc Cases */
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
public final class CaseDetailsEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(CaseDetailsEndpoint.class);

  public static final String CATEGORY_ACCESS_CODE_AUTHENTICATION_ATTEMPT_NOT_FOUND =
      "Category ACCESS_CODE_AUTHENTICATION_ATTEMPT does not exist";
  public static final String ERRORMSG_CASENOTFOUND = "Case not found for";
  public static final String EVENT_REQUIRES_NEW_CASE =
      "Event requested for " + "case %s requires additional data - new Case details";

  private static final String CASE_ID = "%s case id %s";
  private CaseDetailsService caseDetailsService;
  private MapperFacade mapperFacade;

  /** Contructor for CaseDetailsEndpoint */
  @Autowired
  public CaseDetailsEndpoint(
      final CaseDetailsService caseDetailsService,
      final @Qualifier("RHSvcBeanMapper") MapperFacade mapperFacade) {
    this.caseDetailsService = caseDetailsService;
    this.mapperFacade = mapperFacade;
  }

  /**
   * the GET endpoint to find a Case by UUID
   *
   * @return the case found
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/getCaseDetails", method = RequestMethod.GET)
  public ResponseEntity getCaseDetails() {

    // return ResponseEntity.ok(buildDetailedCaseDTO(caseObj, caseevents, iac));

    return null;
  }

  //  /**
  //   * Creates a new event for the Access Code Authorisation Attempt
  //   *
  //   * @param caze Case Object to be used in CaseDTO
  //   * @param caseevents If caseevents exist
  //   * @param iac If IAC exists
  //   * @return CaseDetailsDTO caseDetails object
  //   */
  //  private CaseDetailsDTO buildDetailedCaseDTO(Case caze, boolean caseevents, boolean iac) {
  //    CaseDetailsDTO caseDetailsDTO = mapperFacade.map(caze, CaseDetailsDTO.class);
  //
  //    CaseGroup parentCaseGroup =
  // caseGroupService.findCaseGroupByCaseGroupPK(caze.getCaseGroupFK());
  //    caseDetailsDTO.setCaseGroup(mapperFacade.map(parentCaseGroup, CaseGroupDTO.class));
  //
  //    if (caseevents) {
  //      List<CaseEvent> caseEvents = caseDetailsService.findCaseEventsByCaseFK(caze.getCasePK());
  //      List<CaseEventDTO> caseEventDTOs = mapperFacade.mapAsList(caseEvents, CaseEventDTO.class);
  //      caseDetailsDTO.setCaseEvents(caseEventDTOs);
  //    }
  //
  //    if (!iac) {
  //      caseDetailsDTO.setIac(null);
  //    }
  //
  //    return caseDetailsDTO;
  //  }

}
