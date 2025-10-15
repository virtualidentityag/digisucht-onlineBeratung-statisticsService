package de.caritas.cob.statisticsservice.api.statistics.repository.projection;

import java.time.Instant;
import lombok.Value;

@Value
public class UserEventAggregation {
  String id;
  Integer count;
  Instant lastInteraction;
}
