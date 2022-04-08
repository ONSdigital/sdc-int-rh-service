package uk.gov.ons.ctp.integration.rhsvc.representation;

import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PrintFulfilmentRequestDTO extends FulfilmentRequestDTO {

  @Size(max = 60)
  @LoggingScope(scope = Scope.MASK)
  private String forename;

  @Size(max = 60)
  @LoggingScope(scope = Scope.MASK)
  private String surname;
}
