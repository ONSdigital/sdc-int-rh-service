package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.rhsvc.representation.EqLaunchRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACContextDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.UniqueAccessCodeServiceImpl;

/** The REST endpoint controller for UAC requests. */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/uacs", produces = "application/json")
public class UniqueAccessCodeEndpoint {
  @Autowired private UniqueAccessCodeServiceImpl uacService;

  /**
   * the GET end-point to get RH details for a claim
   *
   * @param uacHash the hashed UAC
   * @return the claim details
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{uacHash}", method = RequestMethod.GET)
  public ResponseEntity<UACContextDTO> getUACClaimContext(
      @PathVariable("uacHash") final String uacHash) throws CTPException {

    log.info("Entering GET getUACClaimContext", kv("uacHash", uacHash));
    UACContextDTO uacDTO = uacService.getUACClaimContext(uacHash);

    log.debug("Exit GET getUACClaimContext");

    return ResponseEntity.ok(uacDTO);
  }

  /**
   * The GET end point to build an EQ Launch URL for a case. It also sends a launch event.
   *
   * @param uacHash the hashed UAC.
   * @param languageCodeString is either 'en' or 'cy'.
   * @param accountServiceUrl
   * @param accountServiceLogoutUrl
   * @param clientIP contains the ip address of the end user.
   * @return the URL to launch the questionnaire for the case.
   * @throws CTPException something went wrong.
   */
  @GetMapping(value = "/{uacHash}/launch")
  public ResponseEntity<String> generateEqLaunchToken(
      @PathVariable("uacHash") final String uacHash,
      @RequestParam(required = true) String languageCode,
      @RequestParam(required = true) String accountServiceUrl,
      @RequestParam(required = true) String accountServiceLogoutUrl,
      @RequestParam(required = true) String clientIP)
      throws CTPException {

    log.info(
        "Entering GET generateEqLaunchToken",
        kv("languageCode", languageCode),
        kv("accountServiceUrl", accountServiceUrl),
        kv("accountServiceLogoutUrl", accountServiceLogoutUrl),
        kv("clientIP", clientIP));

    // Validate the specified language
    Language language = Language.lookup(languageCode);
    if (language == null) {
      throw new CTPException(Fault.BAD_REQUEST, "Invalid language code: '" + languageCode + "'");
    }

    // Generate launch URL
    EqLaunchRequestDTO eqLaunchedDTO =
        EqLaunchRequestDTO.builder()
            .languageCode(language)
            .accountServiceUrl(accountServiceUrl)
            .accountServiceLogoutUrl(accountServiceLogoutUrl)
            .clientIP(clientIP)
            .build();
    String launchURL = uacService.generateEqLaunchToken(uacHash, eqLaunchedDTO);

    log.debug("Exit GET generateEqLaunchToken", kv("clientIP", eqLaunchedDTO.getClientIP()));

    return ResponseEntity.ok(launchURL);
  }
}
