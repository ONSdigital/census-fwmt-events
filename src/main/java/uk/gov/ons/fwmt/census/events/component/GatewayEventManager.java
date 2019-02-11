package uk.gov.ons.fwmt.census.events.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ons.fwmt.census.common.error.GatewayException;

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

  public void triggerEvent(String caseId, String eventType) throws GatewayException {
    if (eventTypes.contains(eventType)) {
      gatewayEventProducer.sendEvent(caseId, eventType);
    } else {
      log.error("Invalid event type: {}", eventType);
    }
  }

}
