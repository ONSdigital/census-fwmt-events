package uk.gov.ons.census.fwmt.events.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.events.config.GatewayRabbitConfig;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;

/**
 * This producer is only really used for managing our acceptance testing
 */
@Slf4j
@Component
public class RabbitMQGatewayEventProducer implements GatewayEventProducer {
  @Value("${app.rabbitmq.gw.exchanges.error}")
  private String errorExchange;

  @Value("${app.rabbitmq.gw.queues.error}")
  private String errorQueue;

  @Qualifier("GW_EVENT_RT")
  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Qualifier("eventExchange")
  @Autowired
  private DirectExchange eventExchange;

  private static final String MSG = "{Could not parse event.}";

  @Retryable
  public void sendEvent(GatewayEventDTO event) {
    try {
      log.info("Sending event to default exchange : {} with queue name {}", eventExchange.getName(), GatewayRabbitConfig.GATEWAY_EVENTS_ROUTING_KEY);

      rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayRabbitConfig.GATEWAY_EVENTS_ROUTING_KEY, event);
    } catch (Exception e) {
      log.error("Failed to log RabbitMQ Event: {}", MSG, e);
    }
  }

  @Override
  public void sendErrorEvent(GatewayErrorEventDTO errorEvent) {
    try {
      log.info("Sending error to the following exchange : {} with queue name {}", errorExchange, errorQueue);
      rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayRabbitConfig.GATEWAY_EVENTS_ROUTING_KEY, errorEvent);
    } catch (Exception e) {
      log.error("Failed to log RabbitMQ Event: {}", MSG, e);
    }
  }
}
