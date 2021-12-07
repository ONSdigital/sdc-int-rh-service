package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.DeliveryChannel;
import uk.gov.ons.ctp.common.domain.ProductGroup;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
  private String packCode;
  private String description;
  private ProductGroup productGroup;
  private DeliveryChannel deliveryChannel;
  private Map<String, Object> metadata;
}
