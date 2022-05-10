package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
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
}
