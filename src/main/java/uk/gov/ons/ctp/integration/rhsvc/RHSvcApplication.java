package uk.gov.ons.ctp.integration.rhsvc;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.cloud.spring.pubsub.support.SubscriberFactory;
import com.google.cloud.spring.pubsub.support.converter.JacksonPubSubMessageConverter;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.PubSubEventSender;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.rest.RestClientConfig;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientApi;

/** The 'main' entry point for the RHSvc SpringBoot Application. */
@Slf4j
@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration", "uk.gov.ons.ctp.common"})
public class RHSvcApplication {

  @Value("${management.metrics.export.stackdriver.project-id}")
  private String stackdriverProjectId;

  @Value("${management.metrics.export.stackdriver.enabled}")
  private boolean stackdriverEnabled;

  @Value("${management.metrics.export.stackdriver.step}")
  private String stackdriverStep;

  @Autowired private AppConfig appConfig;

  @Autowired
  @Qualifier("envoyLimiterCb")
  private CircuitBreaker circuitBreaker;

  /**
   * The main entry point for this application.
   *
   * @param args runtime command line args
   */
  public static void main(final String[] args) {
    SpringApplication.run(RHSvcApplication.class, args);
  }

  @EnableWebSecurity
  public static class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
      // Post requests to the service only work with csrf disabled!
      http.csrf().disable();
    }
  }

  @Bean
  public EventPublisher eventPublisher(
      @Qualifier("pubSubTemplate") PubSubTemplate pubSubTemplate,
      final FirestoreEventPersistence eventPersistence,
      @Qualifier("eventPublisherCbFactory")
          Resilience4JCircuitBreakerFactory circuitBreakerFactory) {

    EventSender sender =
        new PubSubEventSender(pubSubTemplate, appConfig.getMessaging().getPublish().getTimeout());
    CircuitBreaker circuitBreaker = circuitBreakerFactory.create("eventSendCircuitBreaker");
    return EventPublisher.create(sender, eventPersistence, circuitBreaker);
  }

  @Bean
  public PubSubTemplate pubSubTemplate(
      PublisherFactory publisherFactory,
      SubscriberFactory subscriberFactory,
      JacksonPubSubMessageConverter jacksonPubSubMessageConverter) {
    PubSubTemplate pubSubTemplate = new PubSubTemplate(publisherFactory, subscriberFactory);
    pubSubTemplate.setMessageConverter(jacksonPubSubMessageConverter);
    return pubSubTemplate;
  }

  @Bean
  public JacksonPubSubMessageConverter messageConverter() {
    return new JacksonPubSubMessageConverter(customObjectMapper());
  }

  @Bean
  public RateLimiterClient rateLimiterClient() throws CTPException {
    RestClientConfig clientConfig = appConfig.getRateLimiter().getRestClientConfig();
    log.info("Rate Limiter configuration: {}", appConfig.getRateLimiter());
    var statusMapping = clientErrorMapping();
    RestClient restClient =
        new RestClient(clientConfig, statusMapping, HttpStatus.INTERNAL_SERVER_ERROR);
    String password = appConfig.getLogging().getEncryption().getPassword();
    return new RateLimiterClient(restClient, circuitBreaker, password);
  }

  private Map<HttpStatus, HttpStatus> clientErrorMapping() {
    Map<HttpStatus, HttpStatus> mapping = new HashMap<>();
    EnumSet.allOf(HttpStatus.class).stream()
        .filter(s -> s.is4xxClientError())
        .forEach(s -> mapping.put(s, s));
    return mapping;
  }

  /**
   * Custom Object Mapper
   *
   * @return a customer object mapper
   */
  @Bean
  @Primary
  public CustomObjectMapper customObjectMapper() {
    return new CustomObjectMapper();
  }

  @Bean
  StackdriverConfig stackdriverConfig() {
    return new StackdriverConfig() {
      @Override
      public Duration step() {
        return Duration.parse(stackdriverStep);
      }

      @Override
      public boolean enabled() {
        return stackdriverEnabled;
      }

      @Override
      public String projectId() {
        return stackdriverProjectId;
      }

      @Override
      public String get(String key) {
        return null;
      }
    };
  }

  @Bean
  StackdriverMeterRegistry meterRegistry(StackdriverConfig stackdriverConfig) {

    StackdriverMeterRegistry.builder(stackdriverConfig).build();
    return StackdriverMeterRegistry.builder(stackdriverConfig).build();
  }

  @Bean
  public NotificationClientApi notificationClient() {

    return new NotificationClient(
        appConfig.getNotify().getApiKey(), appConfig.getNotify().getBaseUrl());
  }
}
