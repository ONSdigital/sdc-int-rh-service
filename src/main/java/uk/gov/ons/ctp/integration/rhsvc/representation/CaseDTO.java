package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.UUID;
import lombok.Data;

/** Representation of a Case */
@Data
public class CaseDTO {

  private UUID caseId;
  private UUID surveyId;
  private UUID collectionExerciseId;
  private boolean invalid;
  private String refusalReceived;
  @JsonUnwrapped private CaseSampleDTO sample;

  private CaseSampleSensitiveDTO sampleSensitive;
}
