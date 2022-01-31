package uk.gov.ons.ctp.integration.rhsvc.representation;

import lombok.Builder;
import lombok.Data;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;

/** Holds data needed to launch EQ */
@Data
@Builder
public class LaunchDataDTO {

  private UacUpdate uacUpdate;
  private CaseUpdate caseUpdate;
  private CollectionExerciseUpdate collectionExerciseUpdate;
  private SurveyUpdate surveyUpdate;
}
