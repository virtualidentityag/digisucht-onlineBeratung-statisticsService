package de.caritas.cob.statisticsservice.api.statistics.service;

import de.caritas.cob.statisticsservice.api.helper.RegistrationStatisticsDTOConverter;
import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsListResponseDTO;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventRepository;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventTenantAwareRepository;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventStats;
import de.caritas.cob.statisticsservice.api.statistics.service.dto.StatisticContainer;
import de.caritas.cob.statisticsservice.api.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationStatisticsService {

  @Value("${multitenancy.enabled}")
  private Boolean multitenancyEnabled;

  private static final Long TECHNICAL_TENANT_ID = 0L;

  private final @NonNull StatisticsEventRepository statisticsEventRepository;

  private final @NonNull StatisticsEventTenantAwareRepository statisticsEventTenantAwareRepository;

  private final @NonNull RegistrationStatisticsDTOConverter converter;

  public RegistrationStatisticsListResponseDTO fetchRegistrationStatisticsData() {
    StatisticContainer statisticContainer =
        StatisticContainer.builder()
            .archiveDateBySession(getArchiveDateByUser())
            .deleteDateByUser(getDeleteDateByUser())
            .messageStatsByUser(getMessageStatsByUser())
            .videoCallStatsByUser(getVideoCallStatsByUser())
            .bookingStatsByUser(getBookingStatsByUser())
            .build();

    return buildResponseDTO(statisticContainer);
  }

  private RegistrationStatisticsListResponseDTO buildResponseDTO(
      StatisticContainer statisticContainer) {

    RegistrationStatisticsListResponseDTO registrationStatisticsList =
        new RegistrationStatisticsListResponseDTO();
    getRegistrationStatistics().stream()
        .map(event -> converter.convertStatisticsEvent(event, statisticContainer))
        .forEach(registrationStatisticsList::addRegistrationStatisticsItem);

    return registrationStatisticsList;
  }

  private Map<Long, String> getArchiveDateByUser() {
    if (isAllTenantAccessContext()) {
      return statisticsEventRepository.getLatestArchiveDateBySession();
    } else {
      return statisticsEventTenantAwareRepository.getLatestArchiveDateBySession(
          TenantContext.getCurrentTenant());
    }
  }

  private Map<String, String> getDeleteDateByUser() {
    if (isAllTenantAccessContext()) {
      return statisticsEventRepository.getDeleteDateByUser();
    } else {
      return statisticsEventTenantAwareRepository.getDeleteDateByUser(
          TenantContext.getCurrentTenant());
    }
  }

  private Map<String, UserEventStats> getMessageStatsByUser() {
    if (isAllTenantAccessContext()) {
      return statisticsEventRepository.getMessageStatsByUser();
    } else {
      return statisticsEventTenantAwareRepository.getMessageStatsByUser(
          TenantContext.getCurrentTenant());
    }
  }

  private Map<String, UserEventStats> getBookingStatsByUser() {
    if (isAllTenantAccessContext()) {
      return statisticsEventRepository.getBookingStatsByUser();
    } else {
      return statisticsEventTenantAwareRepository.getBookingStatsByUser(
          TenantContext.getCurrentTenant());
    }
  }

  private Map<String, UserEventStats> getVideoCallStatsByUser() {
    if (isAllTenantAccessContext()) {
      return statisticsEventRepository.getVideoCallStatsByUser();
    } else {
      return statisticsEventTenantAwareRepository.getVideoCallStatsByUser(
          TenantContext.getCurrentTenant());
    }
  }

  private List<StatisticsEvent> getRegistrationStatistics() {
    if (isAllTenantAccessContext()) {
      return statisticsEventRepository.getAllRegistrationStatistics();
    } else {
      return statisticsEventTenantAwareRepository.getAllRegistrationStatistics(
          TenantContext.getCurrentTenant());
    }
  }

  private boolean isAllTenantAccessContext() {
    return multitenancyIsDisabled() || TECHNICAL_TENANT_ID.equals(TenantContext.getCurrentTenant());
  }

  private boolean multitenancyIsDisabled() {
    return !multitenancyEnabled;
  }
}
