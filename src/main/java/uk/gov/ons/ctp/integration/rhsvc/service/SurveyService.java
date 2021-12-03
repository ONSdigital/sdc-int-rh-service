package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.List;
import java.util.UUID;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyDTO;

public interface SurveyService {

  /**
   * Get a list of all the surveys.
   * @return list of surveys
   * @throws CTPException on error
   */
  List<SurveyDTO> listSurveys() throws CTPException;

  /**
   * Get a survey by ID.
   *
   * @param surveyId survey ID
   * @return a single survey
   * @throws CTPException on error.
   */
  SurveyDTO survey(UUID surveyId) throws CTPException;
}
