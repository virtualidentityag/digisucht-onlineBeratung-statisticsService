package de.caritas.cob.statisticsservice.api.helper;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsResponseDTO;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticEventsContainer;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.AdviceSeekerAwareMetaData;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.ArchiveMetaData;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.CreateMessageMetaData;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.DeleteAccountMetaData;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.RegistrationMetaData;
import java.util.Collection;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RegistrationStatisticsDTOConverter {

  public RegistrationStatisticsResponseDTO convertStatisticsEvent(
      StatisticsEvent rawEvent, StatisticEventsContainer statisticEventsContainer) {
    RegistrationMetaData metadata = (RegistrationMetaData) rawEvent.getMetaData();
    String maxArchiveDate = findMaxArchiveDate(rawEvent.getSessionId(),
        statisticEventsContainer.getArchiveSessionEvents());
    String deleteAccountDate = findDeleteAccountDate(rawEvent.getUser().getId(),
        statisticEventsContainer.getDeleteAccountEvents());

    return new RegistrationStatisticsResponseDTO()
        .userId(rawEvent.getUser().getId())
        .registrationDate(metadata.getRegistrationDate())
        .age(metadata.getAge())
        .tenantName(metadata.getTenantName())
        .agencyName(metadata.getAgencyName())
        .gender(metadata.getGender())
        .counsellingRelation(metadata.getCounsellingRelation())
        .mainTopicInternalAttribute(metadata.getMainTopicInternalAttribute())
        .topicsInternalAttributes(metadata.getTopicsInternalAttributes())
        .endDate(getEndDate(maxArchiveDate, deleteAccountDate))
        .postalCode(metadata.getPostalCode())
        .referer(metadata.getReferer())
        .attendedVideoCallsCount(
            countEventsPerAdviceSeekerMatchingOnMetadata(rawEvent.getUser().getId(),
                statisticEventsContainer.getVideoCallStartedEvents()))
        .appointmentsBookedCount(
            countEventsPerAdviceSeekerMatchingOnMetadata(rawEvent.getUser().getId(),
                statisticEventsContainer.getBookingCreatedEvents()))
        .consultantMessagesCount(
            countEventsPerAdviceSeekerMatchingOnMetadataByReceiverId(rawEvent.getUser().getId(),
                statisticEventsContainer.getConsultantMessageCreatedEvents()));
  }

  private Integer countEventsPerAdviceSeekerMatchingOnMetadata(String adviceSeekerId,
      Collection<StatisticsEvent> events) {

    return events != null
        ? (int) getCountOfEventsPerAdviceSeekerMatchingOnMetadata(adviceSeekerId, events) : 0;
  }

  private Integer countEventsPerAdviceSeekerMatchingOnMetadataByReceiverId(String adviceSeekerId,
      Collection<StatisticsEvent> events) {

    return events != null
        ? (int) getCountOfEventsPerAdviceSeekerMatchingOnMetadataByReceiverId(adviceSeekerId,
        events) : 0;
  }

  private long getCountOfEventsPerAdviceSeekerMatchingOnMetadata(String adviceSeekerId,
      Collection<StatisticsEvent> statisticsEvents) {
    return statisticsEvents.stream()
        .filter(event -> event.getMetaData() instanceof AdviceSeekerAwareMetaData)
        .filter(event -> {
          AdviceSeekerAwareMetaData metaData = (AdviceSeekerAwareMetaData) event.getMetaData();
          return metaData.getAdviceSeekerId() != null && metaData.getAdviceSeekerId()
              .equals(adviceSeekerId);

        })
        .count();
  }

  private long getCountOfEventsPerAdviceSeekerMatchingOnMetadataByReceiverId(String adviceSeekerId,
      Collection<StatisticsEvent> createMessageEvents) {
    return createMessageEvents.stream()
        .filter(event -> event.getMetaData() instanceof CreateMessageMetaData)
        .filter(event -> {
          CreateMessageMetaData metaData = (CreateMessageMetaData) event.getMetaData();
          return metaData.getReceiverId() != null && metaData.getReceiverId()
              .equals(adviceSeekerId);
        })
        .count();
  }

  private String getEndDate(String maxArchiveDate, String deleteAccountDate) {
    return deleteAccountDate != null ? deleteAccountDate : maxArchiveDate;
  }

  private String findDeleteAccountDate(String userId,
      Collection<StatisticsEvent> deleteAccountEvents) {
    return deleteAccountEvents != null ? deleteAccountEvents.stream()
        .filter(event -> event.getUser() != null && event.getUser().getId().equals(userId))
        .map(event -> ((DeleteAccountMetaData) event.getMetaData()).getDeleteDate())
        .findFirst().orElse(null) : null;
  }

  private String findMaxArchiveDate(Long sessionId,
      Collection<StatisticsEvent> archiveSessionEvents) {
    var maxArchiveEvent = findMaxArchiveSessionEvent(sessionId, archiveSessionEvents);
    if (maxArchiveEvent.isPresent()) {
      ArchiveMetaData metaData = (ArchiveMetaData) maxArchiveEvent.get().getMetaData();
      return metaData.getEndDate();
    }
    return null;
  }

  private Optional<StatisticsEvent> findMaxArchiveSessionEvent(Long sessionId,
      Collection<StatisticsEvent> archiveSessionEvents) {
    return nonNull(archiveSessionEvents) ? archiveSessionEvents.stream()
        .filter(event -> event.getSessionId() != null && event.getSessionId().equals(sessionId))
        .max(comparing(StatisticsEvent::getTimestamp)) : Optional.empty();
  }
}
