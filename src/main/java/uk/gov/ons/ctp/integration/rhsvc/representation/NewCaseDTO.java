package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.time.LocalDate;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCaseDTO {

  private int surveyId;
  @NotNull private UUID collectionExerciseId;

  @NotBlank private String schoolId;
  @NotBlank private String schoolName;

  private boolean consentGivenTest;
  private boolean consentGivenSurvey;

  @NotBlank private String firstName;
  @NotBlank private String lastName;

  @NotBlank private String childFirstName;
  @NotBlank private String childMiddleName;
  @NotBlank private String childLastName;
  @NotNull private LocalDate childDob;

  private String additionalInfo;

  @LoggingScope(scope = Scope.MASK)
  private String childMobileNumber;

  @LoggingScope(scope = Scope.MASK)
  private String childEmailAddress;

  @NotNull
  @Size(max = 20)
  @Pattern(regexp = Constants.PHONENUMBER_RE)
  @LoggingScope(scope = Scope.MASK)
  private String parentMobileNumber;

  @NotBlank
  @LoggingScope(scope = Scope.MASK)
  private String parentEmailAddress;
}
