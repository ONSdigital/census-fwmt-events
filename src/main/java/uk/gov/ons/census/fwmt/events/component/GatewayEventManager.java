package uk.gov.ons.census.fwmt.events.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.census.fwmt.common.error.GatewayException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GatewayEventManager {

  @Autowired
  private GatewayEventProducer gatewayEventProducer;

  private List<String> eventTypes = new ArrayList<>();

  public void addEventTypes(String[] et) {
    for (String e : et) {
      eventTypes.add(e);
    }
  }

  public void triggerEvent(String caseId, String eventType, LocalTime eventTime) throws GatewayException {
    if (eventTypes.contains(eventType)) {
      gatewayEventProducer.sendEvent(caseId, eventType, eventTime);
    } else {
      log.error("Invalid event type: {}", eventType);
    }
  }

}
