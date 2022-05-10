package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.InboundEventIntegrationConfig;

@Profile("mocked-connection-factory")
@Configuration
@Import({InboundEventIntegrationConfig.class, EventReceiverConfiguration.class})
public class MessageSpringConfig {

  /** Spy on Service Activator Message End point */
  @Bean
  public CaseEventReceiver caseEventReceiver(EventFilter eventFilter) {
    CaseEventReceiver receiver = new CaseEventReceiver();
    ReflectionTestUtils.setField(receiver, "eventFilter", eventFilter);
    return Mockito.spy(receiver);
  }

  /** Spy on Service Activator Message End point */
  @Bean
  public CollectionExerciseEventReceiver collectionExerciseEventReceiver() {
    return Mockito.spy(new CollectionExerciseEventReceiver());
  }

  /** Spy on Service Activator Message End point */
  @Bean
  public SurveyEventReceiver surveyEventReceiver() {
    return Mockito.spy(new SurveyEventReceiver());
  }

  /** Spy on Service Activator Message End point */
  @Bean
  public UACEventReceiver uacEventReceiver(AppConfig appConfig) {
    UACEventReceiver receiver = new UACEventReceiver();
    ReflectionTestUtils.setField(receiver, "appConfig", appConfig);
    return Mockito.spy(receiver);
  }
}
