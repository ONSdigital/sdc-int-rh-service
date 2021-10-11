package uk.gov.ons.ctp.integration.rhsvc.representation;

import lombok.Data;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
public class CaseSampleSensitiveDTO {

  @LoggingScope(scope = Scope.MASK)
  private String phoneNumber;
}
