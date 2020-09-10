package uk.gov.ons.census.fwmt.events.producer;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;

import uk.gov.ons.census.fwmt.events.config.GatewayEventQueueConfig;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;

@Component
class RabbitMQGatewayEventProducer implements GatewayEventProducer {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQGatewayEventProducer.class);

  @Autowired
  @Qualifier("GW_EVENT_RT")
  private RabbitTemplate rabbitTemplate;

  @Autowired
  @Qualifier("eventExchange")
  private FanoutExchange eventExchange;

  @Retryable
  public void sendEvent(GatewayEventDTO event) {
    String msg = "{Could not parse event.}";
    try {
      rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayEventQueueConfig.GATEWAY_EVENTS_ROUTING_KEY, event);
    } catch (Exception e) {
      log.error("Failed to log RabbitMQ Event: {}", msg, e);
    }

  }

  @Override
  public void sendErrorEvent(GatewayErrorEventDTO errorEvent) {
    String msg = "{Could not parse event.}";
    try {
      rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayEventQueueConfig.GATEWAY_EVENTS_ROUTING_KEY, errorEvent);
    } catch (Exception e) {
      log.error("Failed to log RabbitMQ Event: {}", msg, e);
    }
  }
}
