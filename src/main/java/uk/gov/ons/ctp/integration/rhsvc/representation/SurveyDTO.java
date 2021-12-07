package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.ons.ctp.common.domain.SurveyType;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDTO extends SurveyLiteDTO {
  private List<ProductDTO> allowedFulfilments;
  private SurveyType surveyType;
}
