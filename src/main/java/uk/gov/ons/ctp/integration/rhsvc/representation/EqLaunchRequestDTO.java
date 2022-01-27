package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Language;

/** This is a request object which holds details about a launched survey. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("serial")
public class EqLaunchRequestDTO implements Serializable {

  @NotNull private Language languageCode;

  @NotNull private String accountServiceUrl;

  @NotNull private String accountServiceLogoutUrl;

  private String clientIP;
}
