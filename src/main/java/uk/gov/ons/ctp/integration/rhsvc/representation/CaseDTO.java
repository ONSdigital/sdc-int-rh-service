package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.Map;
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
  private String caseRef;
  private Map<String, String> sample;
  private Map<String, String> sampleSensitive;
}
