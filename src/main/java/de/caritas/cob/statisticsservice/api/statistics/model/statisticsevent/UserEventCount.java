package de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent;

import lombok.Data;

@Data
public class UserEventCount {
  private String id;
  private Integer count;
}
