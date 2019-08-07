package uk.gov.ons.census.fwmt.events.producer;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.census.fwmt.events.config.GatewayEventQueueConfig;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;
import uk.gov.ons.census.fwmt.events.util.EventUtils;

@Slf4j
@Component
class RabbitMQGatewayEventProducer implements GatewayEventProducer {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  @Qualifier("eventExchange")
  private FanoutExchange eventExchange;

  @Retryable
  public void sendEvent(GatewayEventDTO event) {
    String msg = "{Could not parse event.}";
    try {
      msg = EventUtils.convertToJSON(event);
      rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayEventQueueConfig.GATEWAY_EVENTS_ROUTING_KEY, msg);
    } catch (Exception e) {
      log.error(String.format("Failed to log RabbitMQ Event: {}", msg), e);
    }
  }

  @Override
  public void sendErrorEvent(GatewayErrorEventDTO errorEvent) {
    String msg = "{Could not parse event.}";
    try {
      msg = EventUtils.convertToJSON(errorEvent);
      rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayEventQueueConfig.GATEWAY_EVENTS_ROUTING_KEY, msg);
    } catch (Exception e) {
      log.error(String.format("Failed to log RabbitMQ Event: {}", msg), e);
    }
  }
}
