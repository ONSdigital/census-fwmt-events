package uk.gov.ons.census.fwmt.events.data;

import lombok.*;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayErrorEventDTO {
  @NonNull
  private String caseId;
  private String source;
  private String errorEventType;
  private Date localTime;
  private Map<String, String> metadata;
  private String className;
  private String exceptionName;
  private String message;
}
