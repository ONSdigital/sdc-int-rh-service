package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.SurveyService;

/** The REST controller to deal with Surveys */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/surveys", produces = "application/json")
public class SurveyEndpoint {
  private SurveyService surveyService;

  public SurveyEndpoint(SurveyService surveyService) {
    this.surveyService = surveyService;
  }

  @GetMapping
  public ResponseEntity<List<SurveyDTO>> allSurveys() {
    log.info("Entering GET surveys");
    List<SurveyDTO> result = surveyService.allSurveys();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{surveyId}")
  public ResponseEntity<SurveyDTO> survey(@PathVariable final UUID surveyId) throws CTPException {
    log.info("Entering GET survey by ID {}", kv("surveyId", surveyId));
    SurveyDTO result = surveyService.survey(surveyId);
    return ResponseEntity.ok(result);
  }
}
