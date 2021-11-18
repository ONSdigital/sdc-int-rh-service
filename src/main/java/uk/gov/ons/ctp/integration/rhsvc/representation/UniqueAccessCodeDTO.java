package uk.gov.ons.ctp.integration.rhsvc.representation;

import lombok.Data;

/** Representation of a UAC claim request */
@Data
public class UniqueAccessCodeDTO {

  private String uacHash;
  private boolean active;
  private String qid;
  private boolean receiptReceived;
  private boolean eqLaunched;
  private int wave;
  private SurveyLiteDTO surveyLiteDTO;
  private CollectionExerciseDTO collectionExerciseDTO;
  private CaseDTO caseDTO;
}
