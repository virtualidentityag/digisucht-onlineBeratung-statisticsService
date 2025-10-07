package de.caritas.cob.statisticsservice.api.helper;

import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsResponseDTO;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticContainer;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.RegistrationMetaData;
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
    Integer videoCallCount = statisticContainer.getVideoCallCountsByUser().get(userId);
    Integer bookingCount = statisticContainer.getBookingCountsByUser().get(userId);
    Integer messageCount = statisticContainer.getMessageCountsByUser().get(userId);

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
        .consultantMessagesCount(messageCount);
  }

  private String getEndDate(String deleteAccountDate, String maxArchiveDate) {
    return deleteAccountDate != null ? deleteAccountDate : maxArchiveDate;
  }
}
