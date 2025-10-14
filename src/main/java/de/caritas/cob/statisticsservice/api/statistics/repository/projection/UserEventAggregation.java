package de.caritas.cob.statisticsservice.api.statistics.repository.projection;

import java.time.Instant;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class UserEventAggregation {
  private String id;
  private Integer count;
  private Instant lastInteraction;
}
