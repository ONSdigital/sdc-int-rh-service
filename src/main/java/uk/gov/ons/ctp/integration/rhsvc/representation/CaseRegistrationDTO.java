package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.time.LocalDate;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseRegistrationDTO {
  
  private int surveyId;
  @NotNull private UUID collectionExerciseId;
  
  @NotNull private String schoolId;
  @NotNull private String schoolName;
  
  private boolean consentGivenTest;
  private boolean consentGivenSurvey;
  
  private String firstName;
  private String lastName;
  
  private String childFirstName;
  private String childMiddleName;
  private String childLastName;
  private LocalDate childDob;
  
  private String additionalInfo;
  
  private String childMobileNumber;
  private String childEmailAddress;
  
  private String parentMobileNumber;
  private String parentEmailAddress;
}
