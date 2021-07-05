package uk.gov.ons.ctp.integration.rhsvc.representation;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebformDTO {

  /** enum for category */
  public enum WebformCategory {
    MISSING_INFORMATION,
    TECHNICAL,
    FORM,
    COMPLAINT,
    ADDRESS,
    OTHER
  }

  /** enum for language */
  public enum WebformLanguage {
    EN,
    CY
  }

  @NotNull private WebformCategory category;

  @NotNull private Region region;

  @NotNull private WebformLanguage language;

  @NotNull
  @LoggingScope(scope = Scope.MASK)
  private String name;

  @NotNull
  @LoggingScope(scope = Scope.MASK)
  private String description;

  @NotNull
  @LoggingScope(scope = Scope.MASK)
  private String email;

  private String clientIP;
}
