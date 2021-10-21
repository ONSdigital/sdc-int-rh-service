package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.UUID;
import lombok.Data;

/** Representation of a UAC claim request */
@Data
public class UniqueAccessCodeDTO {

  /** enum for valid case status */
  public enum CaseStatus {
    OK,
    UNLINKED
  }

  private String uacHash;
  private boolean active;
  private CaseStatus caseStatus;
  private String qid;
  private String region;
  private UUID caseId;
  private UUID collectionExerciseId;
  private AddressDTO address;
  private boolean receiptReceived;
  private boolean eqLaunched;
  private int wave;
}
