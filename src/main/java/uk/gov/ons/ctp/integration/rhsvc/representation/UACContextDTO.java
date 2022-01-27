package uk.gov.ons.ctp.integration.rhsvc.representation;

import lombok.Data;

/** Representation of a UAC claim request */
@Data
public class UACContextDTO {

  /** enum for valid case status */
  public enum CaseStatus {
    OK,
    UNLINKED
  }

  private String uacHash;
  private String collectionInstrumentUrl;
  private boolean active;
  private String qid;
  private boolean receiptReceived;
  private boolean eqLaunched;
  private int wave;
  private SurveyLiteDTO survey;
  private CollectionExerciseDTO collectionExercise;
  private CaseDTO collectionCase;
}
