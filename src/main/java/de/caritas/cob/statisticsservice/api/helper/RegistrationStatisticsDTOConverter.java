package de.caritas.cob.statisticsservice.api.helper;

import static java.util.Objects.nonNull;

import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsResponseDTO;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.RegistrationMetaData;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventStats;
import de.caritas.cob.statisticsservice.api.statistics.service.dto.StatisticContainer;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class RegistrationStatisticsDTOConverter {

  public RegistrationStatisticsResponseDTO convertStatisticsEvent(
      StatisticsEvent rawEvent, StatisticContainer statisticContainer) {
    RegistrationMetaData metadata = (RegistrationMetaData) rawEvent.getMetaData();
    String userId = rawEvent.getUser().getId();
    Long sessionId = rawEvent.getSessionId();
    String maxArchiveDate = statisticContainer.getArchiveDateBySession().get(sessionId);
    String deleteAccountDate = statisticContainer.getDeleteDateByUser().get(userId);
    Instant lastActivityDate =
        getLastActivityDateForUser(
            userId,
            statisticContainer.getMessageStatsByUser(),
            statisticContainer.getBookingStatsByUser(),
            statisticContainer.getVideoCallStatsByUser());
    Integer videoCallCount = getCountForUser(userId, statisticContainer.getVideoCallStatsByUser());
    Integer bookingCount = getCountForUser(userId, statisticContainer.getBookingStatsByUser());
    Integer messageCount = getCountForUser(userId, statisticContainer.getMessageStatsByUser());

    return new RegistrationStatisticsResponseDTO()
        .userId(userId)
        .registrationDate(metadata.getRegistrationDate())
        .age(metadata.getAge())
        .tenantName(metadata.getTenantName())
        .agencyName(metadata.getAgencyName())
        .gender(metadata.getGender())
        .counsellingRelation(metadata.getCounsellingRelation())
        .mainTopicInternalAttribute(metadata.getMainTopicInternalAttribute())
        .topicsInternalAttributes(metadata.getTopicsInternalAttributes())
        .endDate(getEndDate(deleteAccountDate, maxArchiveDate))
        .postalCode(metadata.getPostalCode())
        .referer(metadata.getReferer())
        .attendedVideoCallsCount(videoCallCount)
        .appointmentsBookedCount(bookingCount)
        .consultantMessagesCount(messageCount)
        .lastActivityDate(nonNull(lastActivityDate) ? lastActivityDate.toString() : null);
  }

  private String getEndDate(String deleteAccountDate, String maxArchiveDate) {
    return nonNull(deleteAccountDate) ? deleteAccountDate : maxArchiveDate;
  }

  private Integer getCountForUser(String userId, Map<String, UserEventStats> userEventStats) {
    return Optional.ofNullable(userEventStats)
        .map(map -> map.get(userId))
        .map(UserEventStats::getCount)
        .orElse(null);
  }

  private Instant getLastActivityDateForUser(
      String userId,
      Map<String, UserEventStats> messageStats,
      Map<String, UserEventStats> bookingStats,
      Map<String, UserEventStats> videoCallStats) {
    return Stream.of(
            getLastInteraction(messageStats, userId),
            getLastInteraction(bookingStats, userId),
            getLastInteraction(videoCallStats, userId))
        .filter(Objects::nonNull)
        .max(Instant::compareTo)
        .orElse(null);
  }

  private Instant getLastInteraction(Map<String, UserEventStats> userEventStats, String userId) {
    return Optional.ofNullable(userEventStats)
        .map(map -> map.get(userId))
        .map(UserEventStats::getLastInteraction)
        .orElse(null);
  }
}
