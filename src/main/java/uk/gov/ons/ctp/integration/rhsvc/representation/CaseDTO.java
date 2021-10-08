package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.UUID;
import lombok.Data;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

/** Representation of a Case */
@Data
public class CaseDTO {

  private UUID caseId;
  private UUID surveyId;
  private UUID collectionExerciseId;
  private boolean invalid;
  private String refusalReceived;
  @JsonUnwrapped private SampleDTO sample;

  @LoggingScope(scope = Scope.MASK)
  private SampleSensetiveDTO sampleSensitive;
}
