package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.config.EnableIntegration;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

@Profile("mocked-connection-factory")
@Configuration
@EnableIntegration
public class EventReceiverConfiguration {

  @Bean
  public CustomObjectMapper mapper() {
    return new CustomObjectMapper();
  }
}
