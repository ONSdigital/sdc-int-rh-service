package uk.gov.ons.ctp.integration.rhsvc.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.UacEvent;

/** Integration configuration for inbound events. */
@Configuration
public class InboundEventIntegrationConfig {

  private AppConfig appConfig;

  /**
   * Constructor for InboundEventIntegrationConfig
   *
   * @param appConfig centralised configuration
   */
  public InboundEventIntegrationConfig(final AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Bean
  PubSubInboundChannelAdapter caseEventInbound(
      @Qualifier("acceptCaseEvent") MessageChannel channel, PubSubTemplate pubSubTemplate) {
    return makeAdapter(
        channel, pubSubTemplate, appConfig.getQueueConfig().getCaseSubscription(), CaseEvent.class);
  }

  @Bean
  PubSubInboundChannelAdapter uacEventInbound(
      @Qualifier("acceptUACEvent") MessageChannel channel, PubSubTemplate pubSubTemplate) {
    return makeAdapter(
        channel, pubSubTemplate, appConfig.getQueueConfig().getUacSubscription(), UacEvent.class);
  }

  private PubSubInboundChannelAdapter makeAdapter(
      MessageChannel channel,
      PubSubTemplate pubSubTemplate,
      String subscriptionName,
      Class<?> payloadType) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
    adapter.setOutputChannel(channel);
    adapter.setAckMode(AckMode.AUTO);
    adapter.setPayloadType(payloadType);
    return adapter;
  }

  /** @return channel for accepting case events */
  @Bean
  public MessageChannel acceptCaseEvent() {
    DirectChannel channel = new DirectChannel();
    channel.setDatatypes(CaseEvent.class);
    return channel;
  }

  /** @return channel for accepting UAC events */
  @Bean
  public MessageChannel acceptUACEvent() {
    DirectChannel channel = new DirectChannel();
    channel.setDatatypes(UacEvent.class);
    return channel;
  }
}
