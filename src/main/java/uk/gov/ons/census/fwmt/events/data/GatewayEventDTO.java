package uk.gov.ons.census.fwmt.events.data;

import lombok.*;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayEventDTO implements Serializable {
  @NonNull
  private String caseId;
  private String source;
  private String eventType;
  private Date localTime;
  private Map<String, String> metadata;
}
