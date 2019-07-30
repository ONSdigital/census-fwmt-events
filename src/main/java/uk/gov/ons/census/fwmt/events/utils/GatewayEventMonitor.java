package uk.gov.ons.census.fwmt.events.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class GatewayEventMonitor {

  private static final String GATEWAY_EVENTS_EXCHANGE = "Gateway.Events.Exchange";
  private static final String GATEWAY_EVENTS_ROUTING_KEY = "Gateway.Event";
  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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
    enableEventMonitor("localhost", "guest", "guest");
  }

  public void enableEventMonitor(String rabbitLocation, String rabbitUsername, String rabbitPassword) throws IOException, TimeoutException {
    gatewayEventMap = new HashMap<>();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitLocation);
    factory.setUsername(rabbitUsername);
    factory.setPassword(rabbitPassword);
    connection = factory.newConnection();
    channel = connection.createChannel();

    channel.exchangeDeclare(GATEWAY_EVENTS_EXCHANGE, "fanout", true);
    String queueName = channel.queueDeclare().getQueue();
    channel.queueBind(queueName, GATEWAY_EVENTS_EXCHANGE, GATEWAY_EVENTS_ROUTING_KEY);

    Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
          throws IOException {

        JavaTimeModule module = new JavaTimeModule();
        LocalDateTimeDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer(
            DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS"));
        module.addDeserializer(LocalDateTime.class, localDateTimeDeserializer);
        OBJECT_MAPPER = Jackson2ObjectMapperBuilder.json()
            .modules(module)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

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

  public List<GatewayEventDTO> getEventsForEventType(String eventType, int qty) {
    List<GatewayEventDTO> eventsFound = new ArrayList<>();
    Set<String> keys = gatewayEventMap.keySet();

    for (String key: keys) {
      if (key.endsWith(eventType)){
        eventsFound.add(gatewayEventMap.get(key));
      }
    }

    return eventsFound;
  }


  public Collection<GatewayEventDTO> grabEventsTriggered(String eventType, int qty, Long timeOut) {
    Long startTime = System.currentTimeMillis();
    boolean keepChecking = true;
    boolean isAllFound = false;

    List<GatewayEventDTO> eventsFound = null;

    while (keepChecking) {
      eventsFound = getEventsForEventType(eventType, qty);

      isAllFound = (eventsFound.size()>=qty);

      long elapsedTime = System.currentTimeMillis() - startTime;
      if (isAllFound || elapsedTime > timeOut) {
        keepChecking = false;
      } else {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    if (!isAllFound) {
      log.info("Searching for {} event: {} in :-", qty, eventType);
      Set<String> keys = getMapContents();
      for (String key : keys) {
        log.info(key);
      }
    }
    return eventsFound;
  }


  public String getEvent(String eventType) {
    String caseId = null;
    boolean isFound;

    isFound = gatewayEventMap.keySet().contains(eventType);

    if (isFound) {
      System.out.println(gatewayEventMap.values());
    }

    caseId = gatewayEventMap.get(eventType).getCaseId();
//    for (Map.Entry<String, GatewayEventDTO> map: gatewayEventMap.entrySet()) {
//
//     caseId =  map.getValue().;
//
//    }

    return caseId;
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
