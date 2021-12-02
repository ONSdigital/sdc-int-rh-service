package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.List;
import java.util.UUID;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyDTO;

public interface SurveyService {
  List<SurveyDTO> allSurveys();

  SurveyDTO survey(UUID surveyId) throws CTPException;
}
