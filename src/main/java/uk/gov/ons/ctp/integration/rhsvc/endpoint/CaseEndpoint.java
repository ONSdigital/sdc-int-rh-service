package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.NewCaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.CaseServiceImpl;

/** The REST controller to deal with Cases */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
public class CaseEndpoint {
  @Autowired private CaseServiceImpl caseService;

  /**
   * the GET end point to return latest valid Case which matches the supplied sample attribute
   * name/value.
   *
   * @param attributeKey - is the name of the field in the sample data to search by.
   * @param attributeValue - is the value that target case(s) must contain.
   * @return all valid cases which have a sample field which matches the supplied
   *     attributeKey/Value.
   * @throws CTPException something went wrong - thrown by case service
   */
  @RequestMapping(value = "attribute/{attributeKey}/{attributeValue}", method = RequestMethod.GET)
  public ResponseEntity<List<CaseDTO>> findCasesByAttribute(
      @PathVariable(value = "attributeKey") final String attributeKey,
      @PathVariable(value = "attributeValue") final String attributeValue)
      throws CTPException {
    log.info(
        "Entering GET getCaseByAttribute",
        kv("attributeKey", attributeKey),
        kv("attributeValue", attributeValue));

    List<CaseDTO> result = caseService.findCasesBySampleAttribute(attributeKey, attributeValue);
    log.debug(
        "Exit GET getCaseByAttribute",
        kv("attributeName", attributeKey),
        kv("attributeValue", attributeValue));
    return ResponseEntity.ok(result);
  }

  private void validateMatchingCaseId(UUID caseId, UUID dtoCaseId, String dtoName)
      throws CTPException {
    if (!caseId.equals(dtoCaseId)) {
      String message = "The path parameter caseId does not match the caseId in the request body";
      log.warn(message, kv("caseId", caseId), kv("dtoName", dtoName));
      throw new CTPException(Fault.BAD_REQUEST, message);
    }
  }

  /**
   * the POST end point to request an SMS fulfilment for a case.
   *
   * @param caseId is the id for the case.
   * @param requestBodyDTO contains the request body, which specifies the case id, telephone number,
   *     etc.
   * @throws CTPException if the case is not found, or the product cannot be found, or if something
   *     else went wrong.
   */
  @RequestMapping(value = "/{caseId}/fulfilments/sms", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public void fulfilmentRequestBySMS(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    String methodName = "fulfilmentRequestBySMS";
    log.info(
        "Entering POST {}",
        methodName,
        kv("pathParam.caseId", caseId),
        kv("requestBody", requestBodyDTO));

    // Treat an empty clientIP as if it's a null value
    String clientIP = requestBodyDTO.getClientIP();
    if (clientIP != null && clientIP.isBlank()) {
      requestBodyDTO.setClientIP(null);
    }

    validateMatchingCaseId(caseId, requestBodyDTO.getCaseId(), methodName);
    caseService.fulfilmentRequestBySMS(requestBodyDTO);
    log.debug("Exit POST {}", methodName, kv("pathParam.caseId", caseId));
  }

  @RequestMapping(value = "/{caseId}/fulfilments/post", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public void fulfilmentRequestByPost(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    String methodName = "fulfilmentRequestByPost";
    log.info(
        "Entering POST {}",
        methodName,
        kv("pathParam.caseId", caseId),
        kv("requestBody", requestBodyDTO));

    // Treat an empty clientIP as if it's a null value
    String clientIP = requestBodyDTO.getClientIP();
    if (clientIP != null && clientIP.isBlank()) {
      requestBodyDTO.setClientIP(null);
    }

    validateMatchingCaseId(caseId, requestBodyDTO.getCaseId(), methodName);
    caseService.fulfilmentRequestByPost(requestBodyDTO);
    log.debug("Exit POST {}", methodName, kv("pathParam.caseId", caseId));
  }

  @RequestMapping(value = "/new", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public void newCase(@Valid @RequestBody NewCaseDTO caseRegistrationDTO) throws CTPException {
    String methodName = "newCaseRegistration";

    // Only log non-sensitive fields, as current kv logging doesn't support data fields
    log.info(
        "Entering POST {}",
        methodName,
        kv("schoolId", caseRegistrationDTO.getSchoolId()),
        kv("schoolName", caseRegistrationDTO.getSchoolName()),
        kv("consentGivenTest", caseRegistrationDTO.isConsentGivenTest()),
        kv("consentGivenSurvey", caseRegistrationDTO.isConsentGivenSurvey()));

    // Reject if consent not given
    verifyIsTrue(caseRegistrationDTO.isConsentGivenTest(), "consentGivenTest");
    verifyIsTrue(caseRegistrationDTO.isConsentGivenSurvey(), "consentGivenSurvey");

    caseService.sendNewCaseEvent(caseRegistrationDTO);

    log.debug("Exit POST {}", methodName);
  }

  private void verifyIsTrue(boolean checkBoolean, String fieldName) throws CTPException {
    if (!checkBoolean) {
      String message = "The field '" + fieldName + "' must be set to true";
      log.warn(message, kv(fieldName, checkBoolean));
      throw new CTPException(Fault.BAD_REQUEST, message);
    }
  }
}
