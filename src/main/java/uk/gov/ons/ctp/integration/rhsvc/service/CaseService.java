package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.List;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.NewCaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;

/**
 * This class contains business level logic for handling case related functionality for the case
 * endpoint.
 */
public interface CaseService {

  /**
   * Retrieve case(s) which have a sample attribute matching the supplied key/value.
   *
   * @param attributeKey is the name of the attribute to search on.
   * @param attributeValue is the value that the search attribute must equal for a case to be
   *     returned.
   * @return a List containing matching cases. If no cases then the list will be empty.
   * @throws CTPException if anything went wrong.
   */
  List<CaseDTO> findCasesBySampleAttribute(final String attributeKey, final String attributeValue)
      throws CTPException;

  void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException;

  void fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO) throws CTPException;

  void sendNewCaseEvent(NewCaseDTO caseRegistrationDTO) throws CTPException;
}
