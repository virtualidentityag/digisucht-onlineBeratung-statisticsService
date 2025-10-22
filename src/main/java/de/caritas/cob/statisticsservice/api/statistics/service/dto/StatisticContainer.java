package de.caritas.cob.statisticsservice.api.statistics.service.dto;

import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventStats;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class StatisticContainer {
  private Map<Long, String> archiveDateBySession;
  private Map<String, String> deleteDateByUser;
  private Map<String, UserEventStats> messageStatsByUser;
  private Map<String, UserEventStats> videoCallStatsByUser;
  private Map<String, UserEventStats> bookingStatsByUser;
}
