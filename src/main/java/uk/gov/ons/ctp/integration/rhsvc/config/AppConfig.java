package uk.gov.ons.ctp.integration.rhsvc.config;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.config.CustomCircuitBreakerConfig;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;

/** Application Config bean */
@EnableRetry
@Validated
@Configuration
@ConfigurationProperties
@Data
public class AppConfig {
  private Sis sis;
  private Logging logging;
  private QueueConfig queueConfig;
  private MessagingConfig messaging;
  private CustomCircuitBreakerConfig eventPublisherCircuitBreaker;
  private CustomCircuitBreakerConfig envoyLimiterCircuitBreaker;
  private CustomCircuitBreakerConfig webformCircuitBreaker;
  private RateLimiterConfig rateLimiter;
  private EqConfig eq;
  private NotifyConfig notify;
  private WebformConfig webform;
  private LoadsheddingConfig loadshedding;
  private Set<String> surveys;
  private KeyStore keystore;
}
