package uk.gov.ons.census.fwmt.events.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class GatewayEventMonitor {

  private static final String GATEWAY_EVENTS_EXCHANGE = "Gateway.Events.Exchange";
  private static final String GATEWAY_EVENTS_ROUTING_KEY = "Gateway.Event";
  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static Map<String, List<GatewayEventDTO>> gatewayEventMap = null;
  private static Map<String, List<GatewayErrorEventDTO>> gatewayErrorEventMap = null;

  private static List<String> eventToWatch = new ArrayList<>();

  private Channel channel = null;
  private Connection connection = null;

  private String queueName;

  public void tearDownGatewayEventMonitor() {
    if (channel != null) {
      try {
        channel.queueDelete(queueName);
        channel.close();
      } catch (IOException | TimeoutException e) {
        log.error("Problem closing rabbit channel", e);
      }
      channel = null;
    }
    if (connection != null) {
      try {
        connection.close();
      } catch (IOException e) {
        log.error("Problem closing rabbit connection", e);
      }
      connection = null;
    }
    if (gatewayEventMap != null) {
      gatewayEventMap = null;
    }
  }

  public void enableEventMonitor() throws IOException, TimeoutException {
    enableEventMonitor("localhost", "guest", "guest", null);
  }

  public void enableEventMonitor(String rabbitLocation, String rabbitUsername, String rabbitPassword, Integer port) throws IOException, TimeoutException {
    enableEventMonitor(rabbitLocation, rabbitUsername, rabbitPassword, port, Collections.emptyList());
  }

  public void enableEventMonitor(String rabbitLocation, String rabbitUsername, String rabbitPassword, Integer port, List<String> eventsToListen)
          throws IOException, TimeoutException {
    gatewayEventMap = new ConcurrentHashMap<>();
    gatewayErrorEventMap = new ConcurrentHashMap<>();
    eventToWatch.clear();
    eventToWatch.addAll(eventsToListen);

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitLocation);
    if (port != null) {
      factory.setPort(port);
    }
    factory.setUsername(rabbitUsername);
    factory.setPassword(rabbitPassword);
    connection = factory.newConnection();
    channel = connection.createChannel();

    channel.exchangeDeclare(GATEWAY_EVENTS_EXCHANGE, "direct", true);
    queueName = channel.queueDeclare().getQueue();
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
        log.debug(message);

        if (message != null && message.contains("exceptionName")) {
          GatewayErrorEventDTO dto = OBJECT_MAPPER.readValue(message, GatewayErrorEventDTO.class);
          String key = createKey(dto.getCaseId(), dto.getErrorEventType());
          List<GatewayErrorEventDTO> dtoList =  (gatewayErrorEventMap.keySet().contains(key))?gatewayErrorEventMap.get(key):new ArrayList<>();
          dtoList.add(dto);
          gatewayErrorEventMap.put(createKey(dto.getCaseId(), dto.getErrorEventType()), dtoList);
        } else {
          GatewayEventDTO dto = OBJECT_MAPPER.readValue(message, GatewayEventDTO.class);
          if (eventToWatch.isEmpty() || eventToWatch.contains(dto.getEventType())) {
            String key = createKey(dto.getCaseId(), dto.getEventType());
            List<GatewayEventDTO> dtoList =  (gatewayEventMap.keySet().contains(key))?gatewayEventMap.get(key):new ArrayList<>();
            dtoList.add(dto);
            gatewayEventMap.put(createKey(dto.getCaseId(), dto.getEventType()), dtoList);
          }
        }
      }
    };
    channel.basicConsume(queueName, true, consumer);
  }

  public Boolean checkForEvent(String caseID, String eventType) {
    boolean isFound;
    isFound = gatewayEventMap.containsKey(createKey(caseID, eventType));
    if (!isFound) {
    }
    return isFound;
  }

  // TODO Can we use all that lamda stuff to do something funky
  public Boolean checkForErrorEvent(String caseID, String eventType) {
    boolean isFound;
    isFound = gatewayErrorEventMap.containsKey(createKey(caseID, eventType));
    if (!isFound) {
    }
    return isFound;
  }

  public List<GatewayEventDTO> getEventsForEventType(String eventType, int qty) {
    List<GatewayEventDTO> eventsFound = new ArrayList<>();
    Set<String> keys = gatewayEventMap.keySet();

    for (String key : keys) {
      if (key.endsWith(eventType)) {
        eventsFound.addAll(gatewayEventMap.get(key));
      }
    }

    return eventsFound;
  }

  public List<GatewayErrorEventDTO> getErrorEventsForErrorEventType(String eventType, int qty) {
    List<GatewayErrorEventDTO> eventsFound = new ArrayList<>();
    Set<String> keys = gatewayErrorEventMap.keySet();

    for (String key : keys) {
      if (key.endsWith(eventType)) {
        eventsFound.addAll(gatewayErrorEventMap.get(key));
      }
    }

    return eventsFound;
  }

  public String getQueueName() {
    return queueName;
  }

  public Collection<GatewayEventDTO> grabEventsTriggered(String eventType, int qty, Long timeOut) {
    long startTime = System.currentTimeMillis();
    boolean keepChecking = true;
    boolean isAllFound = false;

    List<GatewayEventDTO> eventsFound = null;

    while (keepChecking) {
      eventsFound = getEventsForEventType(eventType, qty);

      isAllFound = (eventsFound.size() >= qty);

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
      Set<String> keys = gatewayEventMap.keySet();
      for (String key : keys) {
        log.info(key);
      }
    }
    return eventsFound;
  }

  public Collection<GatewayErrorEventDTO> grabErrorEventsTriggered(String eventType, int qty, Long timeOut) {
    long startTime = System.currentTimeMillis();
    boolean keepChecking = true;
    boolean isAllFound = false;

    List<GatewayErrorEventDTO> eventsFound = null;

    while (keepChecking) {
      eventsFound = getErrorEventsForErrorEventType(eventType, qty);

      isAllFound = (eventsFound.size() >= qty);

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
      Set<String> keys = gatewayErrorEventMap.keySet();
      for (String key : keys) {
        log.info(key);
      }
    }
    return eventsFound;
  }

  public boolean hasEventTriggered(String caseID, String eventType) {
    return hasEventTriggered(caseID, eventType, 2000l);
  }

  public boolean hasEventTriggered(String caseID, String eventType, Long timeOut) {
    long startTime = System.currentTimeMillis();
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
      log.info("Searching for key:" + caseID + eventType + " in :-");
      Set<String> keys = gatewayEventMap.keySet();
      for (String key : keys) {
        log.info(key);
      }
    }
    return isFound;
  }

  public boolean hasErrorEventTriggered(String caseID, String eventType) {
    return hasErrorEventTriggered(caseID, eventType, 2000l);
  }

  public boolean hasErrorEventTriggered(String caseID, String eventType, Long timeOut) {
    long startTime = System.currentTimeMillis();
    boolean keepChecking = true;
    boolean isFound = false;

    while (keepChecking) {
      isFound = checkForErrorEvent(caseID, eventType);

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
      Set<String> keys = gatewayErrorEventMap.keySet();
      for (String key : keys) {
        log.info(key);
      }
    }
    return isFound;
  }

  private String createKey(String caseID, String eventType) {
    return caseID + " " + eventType;
  }

  public void reset() {
    gatewayEventMap.clear();
    gatewayErrorEventMap.clear();

  }
}