package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.time.LocalDate;
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

  @NotBlank private String schoolId;
  @NotBlank private String schoolName;

  private boolean consentGivenTest;
  private boolean consentGivenSurvey;

  @LoggingScope(scope = Scope.MASK)
  @NotBlank
  private String firstName;

  @LoggingScope(scope = Scope.MASK)
  @NotBlank
  private String lastName;

  @LoggingScope(scope = Scope.MASK)
  @NotBlank
  private String childFirstName;

  @LoggingScope(scope = Scope.MASK)
  private String childMiddleNames;

  @LoggingScope(scope = Scope.MASK)
  @NotBlank
  private String childLastName;

  @LoggingScope(scope = Scope.MASK)
  @NotNull
  private LocalDate childDob;

  @NotNull
  @Size(max = 20)
  @Pattern(regexp = Constants.PHONENUMBER_RE)
  @LoggingScope(scope = Scope.MASK)
  private String mobileNumber;

  @NotBlank
  @LoggingScope(scope = Scope.MASK)
  private String emailAddress;
}
