package uk.gov.ons.census.fwmt.events.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event implements Serializable {
  private String caseId;
  private String source; // FSDR-SERVICE
  private String eventType; //
  private String topic;  // routing-key
  private String className;
  private Date localTime;
  private Map<String, String> metadata;
}
