package uk.gov.ons.census.fwmt.events.component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;

import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO.GatewayErrorEventDTOBuilder;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;
import uk.gov.ons.census.fwmt.events.producer.GatewayEventProducer;

public class GatewayEventManager {
  private static final Logger log = LoggerFactory.getLogger(GatewayEventManager.class);

  @Autowired
  private List<GatewayEventProducer> gatewayEventProducers;

//  private List<String> eventTypes = new ArrayList<>();

//  private List<String> errorEventTypes = new ArrayList<>();

  private String source;

  @Deprecated
  public void addEventTypes(String[] et) {
//    eventTypes.addAll(Arrays.asList(et));
  }

  @Deprecated
  public void addErrorEventTypes(String[] et) {
//    errorEventTypes.addAll(Arrays.asList(et));
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void triggerEvent(String caseId, String eventType){
    triggerEvent(caseId, eventType, new String[0]);
  }

  public void triggerEvent(String caseId, String eventType, String... metadata) {
    Map<String, String> metaDataMap = createMetaDataMap(metadata);
//    if (eventTypes.contains(eventType)) {
      GatewayEventDTO gatewayEventDTO = GatewayEventDTO.builder()
          .caseId(caseId).source(source).eventType(eventType).localTime(new Date()).metadata(metaDataMap)
          .build();
      for (GatewayEventProducer gep : gatewayEventProducers) {
        gep.sendEvent(gatewayEventDTO);
      }
    // } else {
    //   log.error("Invalid event type: {}", eventType);
    // }
  }

  private Map<String, String> createMetaDataMap(String... metadata) {
    int i = 0;
    Map<String, String> metadataMap = new HashMap<>();
    while (i< metadata.length) {
      String key = metadata[i];
      i++;
      if (i<metadata.length) {
        String value = (metadata[i]!=null)?metadata[i]:"null";
        metadataMap.put(key, value);
        i++;
      }
    }
    return metadataMap;
  }

  public void triggerErrorEvent(Class klass, Exception exception, String message, String caseId, String errorEventType) {
    triggerErrorEvent(klass, exception, message, caseId, errorEventType, new String[0]);
  }

  public void triggerErrorEvent(Class klass, String message, String caseId, String errorEventType) {
    triggerErrorEvent(klass, null, message, caseId, errorEventType, new String[0]);
  }

  public void triggerErrorEvent(Class klass, String message, String caseId, String errorEventType, String... metadata) {
    triggerErrorEvent(klass, null, message, caseId, errorEventType, metadata);
  }

  public void triggerErrorEvent(Class klass, Exception exception, String message, String caseId, String errorEventType, String... metadata) {
    Map<String, String> metaDataMap = createMetaDataMap(metadata);
    GatewayErrorEventDTOBuilder builder = GatewayErrorEventDTO.builder()
        .className(klass.getName()).exceptionName((exception != null) ? exception.getClass().getName() : "<NONE>").message(message)
        .caseId(caseId).errorEventType(errorEventType).source(source).localTime(new Date()).metadata(metaDataMap);

//    if (errorEventTypes.contains(errorEventType)) {
      builder.errorEventType(errorEventType);
    // } else {
    //   if (metaDataMap==null) metaDataMap = new HashMap<>();
    //   metaDataMap.put(GatewayEventProducer.INVALID_ERROR_TYPE, errorEventType);
    // }
    for (GatewayEventProducer gep : gatewayEventProducers) {
      gep.sendErrorEvent(builder.build());
    }
  }

}
