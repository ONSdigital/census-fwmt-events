package uk.gov.ons.census.fwmt.events.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.config.GatewayEventQueueConfig;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;

@Slf4j
@Component
class GatewayEventProducer {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  @Qualifier("eventExchange")
  private FanoutExchange eventExchange;

  @Autowired
  private ObjectMapper objectMapper;

  @Retryable
  public void sendEvent(String caseId, String eventType) throws GatewayException {

    GatewayEventDTO gatewayEventDTO = GatewayEventDTO.builder()
        .caseId(caseId).eventType(eventType)
        .build();
    String msg = convertToJSON(gatewayEventDTO);
    rabbitTemplate.convertAndSend(eventExchange.getName(), GatewayEventQueueConfig.GATEWAY_EVENTS_ROUTING_KEY, msg);
    log.info("Message send to events queue");
  }

  protected String convertToJSON(Object dto) throws GatewayException {
    String JSONJobRequest;
    try {
      JSONJobRequest = objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, "Failed to process JSON.", e);
    }
    return JSONJobRequest;
  }
}
