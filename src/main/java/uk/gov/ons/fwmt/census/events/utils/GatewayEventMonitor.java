package uk.gov.ons.fwmt.census.events.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.ons.fwmt.census.events.data.GatewayEventDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class GatewayEventMonitor {

  private static final String GATEWAY_EVENTS_EXCHANGE = "Gateway.Events.Exchange";
  private static final String GATEWAY_EVENTS_ROUTING_KEY = "Gateway.Event";
  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static Map<String, GatewayEventDTO> gatewayEventMap = null;
  private Channel channel = null;
  private Connection connection = null;

  public void tearDownGatewayEventMonitor() throws IOException, TimeoutException {
    if (channel != null) {
      channel.close();
      channel = null;
    }
    if (connection != null) {
      connection.close();
      connection = null;
    }
    if (gatewayEventMap != null) {
      gatewayEventMap = null;
    }
  }

  public void enableEventMonitor() throws IOException, TimeoutException {
    gatewayEventMap = new HashMap<>();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    connection = factory.newConnection();
    channel = connection.createChannel();

    channel.exchangeDeclare(GATEWAY_EVENTS_EXCHANGE, "fanout", true);
    String queueName = channel.queueDeclare().getQueue();
    channel.queueBind(queueName, GATEWAY_EVENTS_EXCHANGE, GATEWAY_EVENTS_ROUTING_KEY);

    Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
          throws IOException {
        String message = new String(body, StandardCharsets.UTF_8);
        GatewayEventDTO dto = OBJECT_MAPPER.readValue(message.getBytes(), GatewayEventDTO.class);
        gatewayEventMap.put(createKey(dto.getCaseId(), dto.getEventType()), dto);
      }
    };
    channel.basicConsume(queueName, true, consumer);
  }

  public Boolean checkForEvent(String caseID, String eventType) {
    boolean isFound;
    isFound = gatewayEventMap.keySet().contains(createKey(caseID, eventType));

    return isFound;
  }

  public boolean hasEventTriggered(String caseID, String eventType) {
    return hasEventTriggered(caseID, eventType, 2000l);
  }

  public boolean hasEventTriggered(String caseID, String eventType, Long timeOut) {
    Long startTime = System.currentTimeMillis();
    boolean keepChecking = true;
    boolean isFound = false;

    while (keepChecking) {
      isFound = checkForEvent(caseID, eventType);

      long elapsedTime = System.currentTimeMillis() - startTime;
      if (isFound || elapsedTime > timeOut) {
        keepChecking = false;
      } else {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    if (!isFound) {
      log.info("Searcjing for key:" + caseID + eventType + " in :-");
      Set<String> keys = getMapContents();
      for (String key : keys) {
        log.info(key);
      }
    }
    return isFound;
  }

  private Set<String> getMapContents() {
    Set<String> eventMapKeys;
    eventMapKeys = gatewayEventMap.keySet();

    return eventMapKeys;
  }

  private String createKey(String caseID, String eventType) {
    return caseID + " " + eventType;
  }
}
