package uk.gov.ons.ctp.integration.rhsvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class SurveyConfig {
  private Set<String> acceptedSurveys;
}
