package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.config.EnableIntegration;

@Profile("mocked-connection-factory")
@Configuration
@EnableIntegration
public class EventReceiverConfiguration {

  /** Setup mock ConnectionFactory for SimpleMessageContainerListener */
  //  @Bean
  //  @Primary
  //  public ConnectionFactory connectionFactory() {
  //
  //    Connection connection = mock(Connection.class);
  //    doAnswer(invocation -> mock(Channel.class)).when(connection).createChannel(anyBoolean());
  //    ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
  //    when(connectionFactory.createConnection()).thenReturn(connection);
  //    return connectionFactory;
  //  }
  //
  //  @Bean
  //  public AmqpAdmin amqpAdmin() {
  //    return mock(AmqpAdmin.class);
  //  }
  //
  //  @Bean
  //  public CustomObjectMapper mapper() {
  //    return new CustomObjectMapper();
  //  }
}
