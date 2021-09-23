package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import uk.gov.ons.ctp.integration.rhsvc.config.InboundEventIntegrationConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CollectionExerciseEventReceiverImpl;

@Profile("mocked-connection-factory")
@Configuration
@Import({InboundEventIntegrationConfig.class, EventReceiverConfiguration.class})
public class CollectionExerciseEventReceiverImplIT_Config {

  /** Spy on Service Activator Message End point */
  @Bean
  public CollectionExerciseEventReceiverImpl collectionExerciseEventReceiver() {
    return Mockito.spy(new CollectionExerciseEventReceiverImpl());
  }
}
