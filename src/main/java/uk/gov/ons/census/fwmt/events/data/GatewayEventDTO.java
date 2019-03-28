package uk.gov.ons.census.fwmt.events.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayEventDTO implements Serializable {

  private String caseId;
  private String eventType;
  private LocalTime localTime;
}
