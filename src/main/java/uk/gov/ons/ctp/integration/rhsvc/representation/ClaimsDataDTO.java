package uk.gov.ons.ctp.integration.rhsvc.representation;

import lombok.Data;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;

/** Representation of a UAC claim request */
@Data
public class ClaimsDataDTO {

//  /** enum for valid case status */
//  public enum CaseStatus {
//    OK,
//    UNLINKED
//  }
//  
  private UacUpdate uacUpdate;
  private CaseUpdate caseUpdate;
  private CollectionExerciseUpdate collectionExerciseUpdate;
  private SurveyUpdate surveyUpdate;
  
  
//  private String uacHash;
//  private String collectionInstrumentUrl;
//  private boolean active;
//  private String qid;
//  private boolean receiptReceived;
//  private boolean eqLaunched;
//  private int wave;
//  private SurveyLiteDTO survey;
//  private CollectionExerciseDTO collectionExercise;
//  private CaseDTO collectionCase;
}
