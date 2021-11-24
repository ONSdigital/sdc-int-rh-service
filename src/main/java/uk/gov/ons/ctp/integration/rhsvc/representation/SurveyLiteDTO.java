package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.UUID;
import lombok.Data;

@Data
public class SurveyLiteDTO {

  private UUID surveyId;
  private String name;
}
