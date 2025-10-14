package de.caritas.cob.statisticsservice.api.statistics.repository.projection;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserEventStats {
  private Integer count;
  private Instant lastInteraction;
}
