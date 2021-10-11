package uk.gov.ons.ctp.integration.rhsvc.representation;

import lombok.Data;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;

@Data
public class SampleDTO {

  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String region;
  private UniquePropertyReferenceNumber uprn;
}
