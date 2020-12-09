package uk.gov.ons.census.fwmt.events.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.events.config.GatewayEventQueueConfig;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;

/**
 * This producer is only really used for managing our acceptance testing
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class RabbitMQGatewayEventProducer implements GatewayEventProducer {

  @Qualifier("GW_EVENT_RT")
  private final RabbitTemplate rabbitTemplate;

  @Qualifier("eventExchange")
  private final FanoutExchange eventExchange;

  private static final String MSG = "{Could not parse event.}";

  @Retryable
  public void sendEvent(GatewayEventDTO event) {
    try {
      rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayEventQueueConfig.GATEWAY_EVENTS_ROUTING_KEY, event);
    } catch (Exception e) {
      log.error("Failed to log RabbitMQ Event: {}", MSG, e);
    }
  }

  @Override
  public void sendErrorEvent(GatewayErrorEventDTO errorEvent) {
    try {
      rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayEventQueueConfig.GATEWAY_EVENTS_ROUTING_KEY, errorEvent);
    } catch (Exception e) {
      log.error("Failed to log RabbitMQ Event: {}", MSG, e);
    }
  }
}
