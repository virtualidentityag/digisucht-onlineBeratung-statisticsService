package de.caritas.cob.statisticsservice.api.statistics.repository.projection;

import java.time.Instant;
import lombok.Value;

@Value
public class UserEventStats {
  Integer count;
  Instant lastInteraction;
}
