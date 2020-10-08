package uk.gov.ons.ctp.integration.rhsvc.representation;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Region;

/** This object holds details from a webform. */
@Data
@NoArgsConstructor
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

  @NotNull private String name;

  @NotNull private String description;

  @NotNull private String email;
}