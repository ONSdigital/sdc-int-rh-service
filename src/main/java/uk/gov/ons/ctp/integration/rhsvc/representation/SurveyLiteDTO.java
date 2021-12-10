package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyLiteDTO {

  private UUID surveyId;
  private String name;
}
