package de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class StatisticContainer {
  private Map<Long, String> archiveDateBySession;
  private Map<String, String> deleteDateByUser;
  private Map<String, Integer> messageCountsByUser;
  private Map<String, Integer> videoCallCountsByUser;
  private Map<String, Integer> bookingCountsByUser;
}
