package uk.gov.ons.fwmt.census.events.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayEventQueueConfig {

  public static final String GATEWAY_EVENTS_ROUTING_KEY = "Gateway.Event";
  private static final String GATEWAY_EVENTS_EXCHANGE = "Gateway.Events.Exchange";

  @Bean
  public FanoutExchange eventExchange() {
    return new FanoutExchange(GATEWAY_EVENTS_EXCHANGE);
  }
}
