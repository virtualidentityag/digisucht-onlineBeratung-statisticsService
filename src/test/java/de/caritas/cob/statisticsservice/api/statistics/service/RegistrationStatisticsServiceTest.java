package de.caritas.cob.statisticsservice.api.statistics.service;

import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.AGENCY_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.ASKER_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.CONSULTING_TYPE_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.TENANT_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.caritas.cob.statisticsservice.api.helper.RegistrationStatisticsDTOConverter;
import de.caritas.cob.statisticsservice.api.model.EventType;
import de.caritas.cob.statisticsservice.api.model.UserRole;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.Agency;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.ConsultingType;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.User;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.RegistrationMetaData;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventRepository;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventTenantAwareRepository;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventStats;
import de.caritas.cob.statisticsservice.api.statistics.service.dto.StatisticContainer;
import de.caritas.cob.statisticsservice.api.tenant.TenantContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationStatisticsServiceTest {
  @InjectMocks RegistrationStatisticsService registrationStatisticsService;
  @Mock StatisticsEventRepository statisticsEventRepository;
  @Mock StatisticsEventTenantAwareRepository statisticsEventTenantAwareRepository;
  @Spy RegistrationStatisticsDTOConverter registrationStatisticsDTOConverter;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(registrationStatisticsService, "multitenancyEnabled", false);
  }

  @Test
  void
      fetchRegistrationStatisticsData_Should_RetrieveAllStatisticsData_When_MultitenancyEnabledIsDisabled() {
    // when
    registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    verify(statisticsEventRepository).getAllRegistrationStatistics();
    verify(statisticsEventRepository).getMessageStatsByUser();
    verify(statisticsEventRepository).getBookingStatsByUser();
    verify(statisticsEventRepository).getVideoCallStatsByUser();
    verify(statisticsEventRepository).getLatestArchiveDateBySession();
    verify(statisticsEventRepository).getDeleteDateByUser();
    verifyNoMoreInteractions(statisticsEventRepository);

    verifyNoInteractions(statisticsEventTenantAwareRepository);
  }

  @Test
  void
      fetchRegistrationStatisticsData_Should_RetrieveTenantAwareStatisticsData_When_MultitenancyEnabledIsEnabled() {
    // given
    ReflectionTestUtils.setField(registrationStatisticsService, "multitenancyEnabled", true);
    TenantContext.setCurrentTenant(TENANT_ID);

    // when
    registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    verify(statisticsEventTenantAwareRepository).getAllRegistrationStatistics(TENANT_ID);
    verify(statisticsEventTenantAwareRepository).getMessageStatsByUser(TENANT_ID);
    verify(statisticsEventTenantAwareRepository).getBookingStatsByUser(TENANT_ID);
    verify(statisticsEventTenantAwareRepository).getVideoCallStatsByUser(TENANT_ID);
    verify(statisticsEventTenantAwareRepository).getLatestArchiveDateBySession(TENANT_ID);
    verify(statisticsEventTenantAwareRepository).getDeleteDateByUser(TENANT_ID);
    verifyNoMoreInteractions(statisticsEventRepository);

    verifyNoInteractions(statisticsEventRepository);

    TenantContext.clear();
  }

  @Test
  void
      fetchRegistrationStatisticsData_Should_RetrieveExpectedData_When_matchingStatisticsAreAvailable() {
    // given
    givenRegistrationStatistics();

    // when
    var result = registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    verify(registrationStatisticsDTOConverter)
        .convertStatisticsEvent(any(StatisticsEvent.class), any(StatisticContainer.class));

    assertThat(result.getRegistrationStatistics().get(0).getUserId(), is(ASKER_ID));
    assertThat(
        result.getRegistrationStatistics().get(0).getRegistrationDate(),
        is("2022-09-15T09:14:45Z"));
    assertThat(result.getRegistrationStatistics().get(0).getAge(), is(26));
    assertThat(result.getRegistrationStatistics().get(0).getGender(), is("FEMALE"));
    assertThat(
        result.getRegistrationStatistics().get(0).getMainTopicInternalAttribute(), is("alk"));
    assertThat(
        result.getRegistrationStatistics().get(0).getTopicsInternalAttributes(),
        is(List.of("alk", "drogen")));
    assertThat(result.getRegistrationStatistics().get(0).getPostalCode(), is("12345"));
    assertThat(
        result.getRegistrationStatistics().get(0).getCounsellingRelation(), is("SELF_COUNSELLING"));
  }

  @Test
  void fetchRegistrationStatisticsData_Should_addEndDate_When_ArchiveDateIsAvailable() {
    // given
    givenRegistrationStatistics();
    givenArchiveSessionEvents();

    // when
    var result = registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    verify(registrationStatisticsDTOConverter)
        .convertStatisticsEvent(any(StatisticsEvent.class), any(StatisticContainer.class));

    assertThat(result.getRegistrationStatistics().get(0).getEndDate(), is("end date 1"));
  }

  @Test
  void fetchRegistrationStatisticsData_Should_addDeleteEndDate_When_DeleteDateIsAvailable() {
    // given
    givenRegistrationStatistics();
    givenArchiveSessionEvents();
    givenDeleteSessionEvents();

    // when
    var result = registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    verify(registrationStatisticsDTOConverter)
        .convertStatisticsEvent(any(StatisticsEvent.class), any(StatisticContainer.class));

    assertThat(result.getRegistrationStatistics().get(0).getEndDate(), is("end date delete 1"));
  }

  @Test
  void fetchRegistrationStatisticsData_Should_addMessageCount_When_MessageStatsAreAvailable() {
    // given
    givenRegistrationStatistics();
    givenMessageStats();

    // when
    var result = registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    verify(registrationStatisticsDTOConverter)
        .convertStatisticsEvent(any(StatisticsEvent.class), any(StatisticContainer.class));

    assertThat(result.getRegistrationStatistics().get(0).getConsultantMessagesCount(), is(5));
  }

  @Test
  void fetchRegistrationStatisticsData_Should_addVideoCallCount_When_VideoCallStatsAreAvailable() {
    // given
    givenRegistrationStatistics();
    givenVideoCallStats();

    // when
    var result = registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    verify(registrationStatisticsDTOConverter)
        .convertStatisticsEvent(any(StatisticsEvent.class), any(StatisticContainer.class));

    assertThat(result.getRegistrationStatistics().get(0).getAttendedVideoCallsCount(), is(3));
  }

  @Test
  void fetchRegistrationStatisticsData_Should_addBookingCount_When_BookingStatsAreAvailable() {
    // given
    givenRegistrationStatistics();
    givenBookingStats();

    // when
    var result = registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    verify(registrationStatisticsDTOConverter)
        .convertStatisticsEvent(any(StatisticsEvent.class), any(StatisticContainer.class));

    assertThat(result.getRegistrationStatistics().get(0).getAppointmentsBookedCount(), is(2));
  }

  @Test
  void fetchRegistrationStatisticsData_Should_addLastActivityDate_When_MultipleStatsAreAvailable() {
    // given
    givenRegistrationStatistics();

    Instant messageDate = Instant.parse("2021-05-08T10:30:20Z");
    Instant videoCallDate = Instant.parse("2021-05-25T15:00:00Z"); // ← MAX!
    Instant bookingDate = Instant.parse("2021-05-10T12:00:00Z");

    Map<String, UserEventStats> messageStats = Map.of(ASKER_ID, new UserEventStats(5, messageDate));
    Map<String, UserEventStats> videoCallStats =
        Map.of(ASKER_ID, new UserEventStats(3, videoCallDate));
    Map<String, UserEventStats> bookingStats = Map.of(ASKER_ID, new UserEventStats(2, bookingDate));

    when(statisticsEventRepository.getMessageStatsByUser()).thenReturn(messageStats);
    when(statisticsEventRepository.getVideoCallStatsByUser()).thenReturn(videoCallStats);
    when(statisticsEventRepository.getBookingStatsByUser()).thenReturn(bookingStats);

    // when
    var result = registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    assertThat(
        result.getRegistrationStatistics().get(0).getLastActivityDate(),
        is("2021-05-25T15:00:00Z")); // Video Call ist das neueste
  }

  @Test
  void fetchRegistrationStatisticsData_Should_notAddCounts_When_NoStatsAreAvailable() {
    // given
    givenRegistrationStatistics();
    when(statisticsEventRepository.getMessageStatsByUser()).thenReturn(Map.of());
    when(statisticsEventRepository.getVideoCallStatsByUser()).thenReturn(Map.of());
    when(statisticsEventRepository.getBookingStatsByUser()).thenReturn(Map.of());

    // when
    var result = registrationStatisticsService.fetchRegistrationStatisticsData();

    // then
    assertThat(
        result.getRegistrationStatistics().get(0).getConsultantMessagesCount(), is(nullValue()));
    assertThat(
        result.getRegistrationStatistics().get(0).getAttendedVideoCallsCount(), is(nullValue()));
    assertThat(
        result.getRegistrationStatistics().get(0).getAppointmentsBookedCount(), is(nullValue()));
    assertThat(result.getRegistrationStatistics().get(0).getLastActivityDate(), is(nullValue()));
  }

  private void givenMessageStats() {
    Map<String, UserEventStats> messageStats =
        Map.of(ASKER_ID, new UserEventStats(5, Instant.parse("2021-05-08T10:30:20Z")));
    when(statisticsEventRepository.getMessageStatsByUser()).thenReturn(messageStats);
  }

  private void givenVideoCallStats() {
    Map<String, UserEventStats> videoCallStats =
        Map.of(ASKER_ID, new UserEventStats(3, Instant.parse("2021-05-25T15:00:00Z")));
    when(statisticsEventRepository.getVideoCallStatsByUser()).thenReturn(videoCallStats);
  }

  private void givenBookingStats() {
    Map<String, UserEventStats> bookingStats =
        Map.of(ASKER_ID, new UserEventStats(2, Instant.parse("2021-05-10T12:00:00Z")));
    when(statisticsEventRepository.getBookingStatsByUser()).thenReturn(bookingStats);
  }

  private void givenRegistrationStatistics() {
    List<StatisticsEvent> testData = new ArrayList<>();
    Object metaData =
        RegistrationMetaData.builder()
            .registrationDate("2022-09-15T09:14:45Z")
            .age(26)
            .gender("FEMALE")
            .mainTopicInternalAttribute("alk")
            .topicsInternalAttributes(List.of("alk", "drogen"))
            .postalCode("12345")
            .tenantId(1L)
            .counsellingRelation("SELF_COUNSELLING")
            .build();
    testData.add(
        StatisticsEvent.builder()
            .sessionId(1L)
            .eventType(EventType.REGISTRATION)
            .user(User.builder().userRole(UserRole.ASKER).id(ASKER_ID).build())
            .consultingType(ConsultingType.builder().id(CONSULTING_TYPE_ID).build())
            .agency(Agency.builder().id(AGENCY_ID).build())
            .timestamp(Instant.now())
            .metaData(metaData)
            .build());

    when(statisticsEventRepository.getAllRegistrationStatistics()).thenReturn(testData);
  }

  private void givenArchiveSessionEvents() {
    Map<Long, String> archiveEvents = Map.of(1L, "end date 1", 99L, "end date 2");
    when(statisticsEventRepository.getLatestArchiveDateBySession()).thenReturn(archiveEvents);
  }

  private void givenDeleteSessionEvents() {
    Map<String, String> deleteAccountEvents =
        Map.of(ASKER_ID, "end date delete 1", "99", "end date 2");
    when(statisticsEventRepository.getDeleteDateByUser()).thenReturn(deleteAccountEvents);
  }
}
