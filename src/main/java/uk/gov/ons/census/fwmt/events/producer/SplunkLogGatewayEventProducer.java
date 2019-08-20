package uk.gov.ons.census.fwmt.events.producer;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;
import uk.gov.ons.census.fwmt.events.util.EventUtils;

@Slf4j
@Component
public class SplunkLogGatewayEventProducer implements GatewayEventProducer {

  @Override
  public void sendEvent(GatewayEventDTO event) {
    String msg = "{Could not parse event.}";
    try {
      msg = EventUtils.convertToJSON(event);
      log.info("{} event: {}", event.getSource(), msg);
    } catch (Exception e) {
      log.error("Failed to log RabbitMQ Event: {}", msg, e);
    }
  }

  @Override
  public void sendErrorEvent(GatewayErrorEventDTO errorEvent) {
    String msg = "{Could not parse event.}";
    try {
      msg = EventUtils.convertToJSON(errorEvent);
      if (errorEvent.getMetadata()!=null && errorEvent.getMetadata().containsKey(GatewayEventProducer.INVALID_ERROR_TYPE)) {
        log.error("Invalid event type: {}", errorEvent.getMetadata().get(GatewayEventProducer.INVALID_ERROR_TYPE));
      }
      log.info("{} {} error event: {}", errorEvent.getSource(), errorEvent.getErrorEventType(), msg);
    } catch (Exception e) {
      log.error("Failed to log RabbitMQ Event: {}", msg, e);
    }
  }
}
