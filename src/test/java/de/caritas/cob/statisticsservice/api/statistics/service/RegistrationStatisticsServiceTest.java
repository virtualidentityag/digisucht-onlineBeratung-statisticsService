package de.caritas.cob.statisticsservice.api.statistics.service;

import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.AGENCY_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.ASKER_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.CONSULTING_TYPE_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.TENANT_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticContainer;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.User;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.RegistrationMetaData;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventRepository;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventTenantAwareRepository;
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
    verify(statisticsEventRepository).getMessageCountsByUser();
    verify(statisticsEventRepository).getBookingCountsByUser();
    verify(statisticsEventRepository).getVideoCallCountsByUser();
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
    verify(statisticsEventTenantAwareRepository).getMessageCountsByUser(TENANT_ID);
    verify(statisticsEventTenantAwareRepository).getBookingCountsByUser(TENANT_ID);
    verify(statisticsEventTenantAwareRepository).getVideoCallCountsByUser(TENANT_ID);
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
