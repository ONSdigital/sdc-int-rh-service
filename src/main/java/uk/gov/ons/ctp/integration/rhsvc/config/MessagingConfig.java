package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class MessagingConfig {

  private PublishConfig publish;

  @Data
  public static class PublishConfig {
    private int timeout;
  }
}
