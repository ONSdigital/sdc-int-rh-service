package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.log.LoggingScope;
import uk.gov.ons.ctp.common.log.Scope;
import uk.gov.ons.ctp.integration.rhsvc.util.UniquePropertyReferenceNumberSerializer;

/** Representation of address data */
@Data
public class AddressDTO {

  @JsonSerialize(using = UniquePropertyReferenceNumberSerializer.class)
  @NotNull
  private UniquePropertyReferenceNumber uprn;

  @NotNull private String addressLine1;

  @LoggingScope(scope = Scope.MASK)
  private String addressLine2;

  @LoggingScope(scope = Scope.MASK)
  private String addressLine3;

  @LoggingScope(scope = Scope.MASK)
  private String townName;

  @NotNull private String postcode;
}
