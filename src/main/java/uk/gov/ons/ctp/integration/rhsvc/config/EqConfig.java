package uk.gov.ons.ctp.integration.rhsvc.config;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EqConfig {
  @NotBlank private String responseIdSalt;
}
