package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseRegistrationDTO {
  
  private int surveyId;
  private UUID collectionExerciseId;
  
  private String schoolId;
  private String schoolName;
  
  private boolean consentGivenTest;
  private boolean consentGivenSurvey;
  
  private String firstName;
  private String lastName;
  
  private String childFirstName;
  private String childMiddleName;
  private String childLastName;
  private Date childDob;
  
  private String additionalInfo;
  
  private String childMobileNumber;
  private String childEmailAddress;
  
  private String parentMobileNumber;
  private String parentEmailAddress;
}
