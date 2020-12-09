package uk.gov.ons.census.fwmt.events.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO;
import uk.gov.ons.census.fwmt.events.data.GatewayErrorEventDTO.GatewayErrorEventDTOBuilder;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;
import uk.gov.ons.census.fwmt.events.producer.GatewayEventProducer;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class GatewayEventManager {

  private final List<GatewayEventProducer> gatewayEventProducers;

  private String source;

  @Deprecated
  public void addEventTypes(String[] et) {
    log.error("Method deprecated - don't use ");
  }

  @Deprecated
  public void addErrorEventTypes(String[] et) {
    log.error("Method deprecated - don't use ");
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void triggerEvent(String caseId, String eventType){
    triggerEvent(caseId, eventType, new String[0]);
  }

  public void triggerEvent(String caseId, String eventType, String... metadata) {
    Map<String, String> metaDataMap = createMetaDataMap(metadata);
      GatewayEventDTO gatewayEventDTO = GatewayEventDTO.builder()
          .caseId(caseId).source(source).eventType(eventType).localTime(new Date()).metadata(metaDataMap)
          .build();
      for (GatewayEventProducer gep : gatewayEventProducers) {
        gep.sendEvent(gatewayEventDTO);
      }
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

      builder.errorEventType(errorEventType);
    for (GatewayEventProducer gep : gatewayEventProducers) {
      gep.sendErrorEvent(builder.build());
    }
  }
}
