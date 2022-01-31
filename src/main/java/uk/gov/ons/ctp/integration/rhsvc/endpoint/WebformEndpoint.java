package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import java.util.UUID;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.WebformServiceImpl;

/** The REST endpoint controller for the webform endpoint. */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class WebformEndpoint {
  @Autowired private WebformServiceImpl webformService;

  @RequestMapping(value = "/webform", method = RequestMethod.POST)
  public void webformCapture(@Valid @RequestBody WebformDTO webform) throws CTPException {
    log.info("Entering POST webformCapture", kv("requestBody", webform));
    UUID notificationId = webformService.sendWebformEmail(webform);
    log.info("Exit POST webformCapture", kv("notificationId", notificationId));
  }
}
