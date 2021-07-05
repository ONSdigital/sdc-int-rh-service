package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

/** This is a service layer class, which performs RH business level logic for webform requests. */
@Slf4j
@Service
public class WebformServiceImpl implements WebformService {
  private static final String TEMPLATE_FULL_NAME = "respondent_full_name";
  private static final String TEMPLATE_EMAIL = "respondent_email";
  private static final String TEMPLATE_REGION = "respondent_region";
  private static final String TEMPLATE_CATEGORY = "respondent_category";
  private static final String TEMPLATE_DESCRIPTION = "respondent_description";

  private NotificationClientApi notificationClient;
  private CircuitBreaker webformCircuitBreaker;

  private AppConfig appConfig;

  @Autowired private RateLimiterClient rateLimiterClient;

  /**
   * Constructor for WebformServiceImpl
   *
   * @param notificationClient Gov.uk Notify service client
   * @param webformCircuitBreaker circuit breaker for calls to GOV.UK notify
   * @param appConfig centralised configuration properties
   */
  @Autowired
  public WebformServiceImpl(
      final NotificationClientApi notificationClient,
      final @Qualifier("webformCb") CircuitBreaker webformCircuitBreaker,
      final AppConfig appConfig) {
    this.notificationClient = notificationClient;
    this.webformCircuitBreaker = webformCircuitBreaker;
    this.appConfig = appConfig;
  }

  @Override
  public UUID sendWebformEmail(WebformDTO webform) throws CTPException {
    checkWebformRateLimit(webform.getClientIP());
    return doSendWebFormEmail(webform);
  }

  /**
   * Since it is possible the GOV.UK notify service could either fail or be slow, we use a circuit
   * breaker wrapper here to fail fast to prevent the user waiting for a failed response over a long
   * time, and also to protect the RHSvc (thread) resources from getting tied up.
   *
   * <p>If GOV.UK notify response is too slow an error response will be returned back to the caller.
   * If GOV.UK notify returns an error, or is down, an error response will go back to the caller,
   * and for repeated failures the circuit breaker will do it's usual fail-fast mechanism.
   *
   * @param webform webform DTO
   * @return the notification ID returned by the GOV.UK notify service.
   * @throws RuntimeException a wrapper around any error response exception, which could typically
   *     be a failure from GOV.UK Notify, or a circuit breaker timeout or fail-fast.
   */
  private UUID doSendWebFormEmail(WebformDTO webform) {
    return this.webformCircuitBreaker.run(
        () -> {
          SendEmailResponse response = send(webform);
          return response.getNotificationId();
        },
        throwable -> {
          String msg = throwable.getMessage();
          if (throwable instanceof TimeoutException) {
            int timeout = appConfig.getWebformCircuitBreaker().getTimeout();
            msg = "call timed out, took longer than " + timeout + " seconds to complete";
          }
          log.info("Send within circuit breaker failed: {}", msg);
          throw new RuntimeException(throwable);
        });
  }

  private SendEmailResponse send(WebformDTO webform) {
    String emailToAddress =
        WebformDTO.WebformLanguage.CY.equals(webform.getLanguage())
            ? appConfig.getWebform().getEmailCy()
            : appConfig.getWebform().getEmailEn();
    String reference = UUID.randomUUID().toString();

    try {
      SendEmailResponse response =
          notificationClient.sendEmail(
              appConfig.getWebform().getTemplateId(),
              emailToAddress,
              templateValues(webform),
              reference);
      log.debug(
          "Gov Notify sendEmail response received",
          kv("reference", reference),
          kv("notificationId", response.getNotificationId()),
          kv("templateId", response.getTemplateId()),
          kv("templateVersion", response.getTemplateVersion()));
      return response;
    } catch (NotificationClientException ex) {
      log.error(
          "Gov Notify sendEmail error",
          kv("reference", reference),
          kv("webform", webform),
          kv("emailToAddress", emailToAddress),
          kv("status", ex.getHttpResult()),
          kv("message", ex.getMessage()),
          ex);
      throw new RuntimeException("Gov Notify sendEmail error", ex);
    }
  }

  private Map<String, String> templateValues(WebformDTO webform) {
    Map<String, String> personalisation = new HashMap<>();
    personalisation.put(TEMPLATE_FULL_NAME, webform.getName());
    personalisation.put(TEMPLATE_EMAIL, webform.getEmail());
    personalisation.put(TEMPLATE_REGION, webform.getRegion().name());
    personalisation.put(TEMPLATE_CATEGORY, webform.getCategory().name());
    personalisation.put(TEMPLATE_DESCRIPTION, webform.getDescription());
    return personalisation;
  }

  private void checkWebformRateLimit(String ipAddress) throws CTPException {
    if (appConfig.getRateLimiter().isEnabled()) {
      log.debug("Invoking rate limiter for webform", kv("ipAddress", ipAddress));
      // Do rest call to rate limiter
      rateLimiterClient.checkWebformRateLimit(Domain.RH, ipAddress);
    } else {
      log.info("Rate limiter client is disabled");
    }
  }
}
