package uk.gov.ons.census.fwmt.events.dto;

import java.util.Date;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorEvent {
  private String caseId;
  private String source;
  private String errorEventType;
  private Date localTime;
  private String topic;
  private Map<String, String> metadata;
  private String className;
  private String exceptionName;
  private String message;
}
