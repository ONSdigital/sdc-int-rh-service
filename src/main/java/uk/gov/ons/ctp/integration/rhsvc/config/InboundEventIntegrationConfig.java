package uk.gov.ons.ctp.integration.rhsvc.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
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
  public PubSubInboundChannelAdapter caseEventInbound(
      @Qualifier("acceptCaseEvent") MessageChannel channel, PubSubTemplate pubSubTemplate) {
    return makeAdapter(
        channel, pubSubTemplate, appConfig.getQueueConfig().getCaseSubscription(), CaseEvent.class);
  }

  @Bean
  public PubSubInboundChannelAdapter uacEventInbound(
      @Qualifier("acceptUACEvent") MessageChannel channel, PubSubTemplate pubSubTemplate) {
    return makeAdapter(
        channel, pubSubTemplate, appConfig.getQueueConfig().getUacSubscription(), UacEvent.class);
  }

  @Bean
  public PubSubInboundChannelAdapter surveyEventInbound(
      @Qualifier("acceptSurveyUpdateEvent") MessageChannel channel, PubSubTemplate pubSubTemplate) {
    return makeAdapter(
        channel,
        pubSubTemplate,
        appConfig.getQueueConfig().getSurveySubscription(),
        SurveyUpdateEvent.class);
  }

  @Bean
  public PubSubInboundChannelAdapter collectionExerciseEventInbound(
      @Qualifier("acceptCollectionExerciseEvent") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(
        channel,
        pubSubTemplate,
        appConfig.getQueueConfig().getCollectionExerciseSubscription(),
        CollectionExerciseUpdateEvent.class);
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

  /** @return channel for accepting Survey Update events */
  @Bean
  public MessageChannel acceptSurveyUpdateEvent() {
    DirectChannel channel = new DirectChannel();
    channel.setDatatypes(SurveyUpdateEvent.class);
    return channel;
  }

  /** @return channel for accepting CollectionExercise events */
  @Bean
  public MessageChannel acceptCollectionExerciseEvent() {
    DirectChannel channel = new DirectChannel();
    channel.setDatatypes(CollectionExerciseUpdateEvent.class);
    return channel;
  }
}
