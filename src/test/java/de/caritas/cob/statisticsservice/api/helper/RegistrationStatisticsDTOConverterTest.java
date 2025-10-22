package de.caritas.cob.statisticsservice.api.helper;

import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.AGENCY_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.ASKER_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.CONSULTING_TYPE_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import de.caritas.cob.statisticsservice.api.model.EventType;
import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsResponseDTO;
import de.caritas.cob.statisticsservice.api.model.UserRole;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.Agency;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.ConsultingType;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.User;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.RegistrationMetaData;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventStats;
import de.caritas.cob.statisticsservice.api.statistics.service.dto.StatisticContainer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationStatisticsDTOConverterTest {

  @InjectMocks RegistrationStatisticsDTOConverter registrationStatisticsDTOConverter;

  @Test
  void convertStatisticsEvent_Should_convertToRegistrationStatisticResponse() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(1L);

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(Map.of())
                .deleteDateByUser(Map.of())
                .messageStatsByUser(Map.of())
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());

    // then
    assertThat(result.getUserId(), is(ASKER_ID));
    assertThat(result.getRegistrationDate(), is("2022-09-15T09:14:45Z"));
    assertThat(result.getAge(), is(26));
    assertThat(result.getGender(), is("FEMALE"));
    assertThat(result.getMainTopicInternalAttribute(), is("alk"));
    assertThat(result.getTopicsInternalAttributes(), is(List.of("alk", "drogen")));
    assertThat(result.getPostalCode(), is("12345"));
    assertThat(result.getCounsellingRelation(), is("SELF_COUNSELLING"));
    assertThat(result.getTenantName(), is("tenantName"));
    assertThat(result.getAgencyName(), is("agencyName"));
    assertThat(result.getReferer(), is("aReferer"));
  }

  @Test
  void convertStatisticsEvent_Should_notFail_When_archiveSessionEventsAreNull() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(1L);

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(Map.of())
                .deleteDateByUser(Map.of())
                .messageStatsByUser(Map.of())
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());

    // then
    assertThat(result.getEndDate(), is(nullValue()));
  }

  @Test
  void
      convertStatisticsEvent_Should_addNewestArchiveSessionEndDate_When_multipleArchiveSessionEventsAreAvailable() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(1L);
    Map<Long, String> archiveDates = givenArchiveDateBySession();

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(archiveDates)
                .deleteDateByUser(Map.of())
                .messageStatsByUser(Map.of())
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());
    // then
    assertThat(result.getEndDate(), is("2 end date for session 1"));
  }

  @Test
  void
      convertStatisticsEvent_Should_takeDeleteDateAsSessionEndDate_When_multipleArchiveSessionEventsAreAvailableAndDeleteDateExists() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(1L);
    Map<Long, String> archiveDates = givenArchiveDateBySession();
    Map<String, String> deleteDates = givenDeleteDateByUser();

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(archiveDates)
                .deleteDateByUser(deleteDates)
                .messageStatsByUser(Map.of())
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());

    // then
    assertThat(result.getEndDate(), is("delete date for user 1"));
  }

  @Test
  void
      convertStatisticsEvent_Should_addArchiveSessionEndDate_When_onlyOneArchiveSessionEventIsAvailable() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(2L);
    Map<Long, String> archiveDates = givenArchiveDateBySession();

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(archiveDates)
                .deleteDateByUser(Map.of())
                .messageStatsByUser(Map.of())
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());

    // then
    assertThat(result.getEndDate(), is("end date for session 2"));
  }

  @Test
  void
      convertStatisticsEvent_Should_notAddArchiveSessionEndDate_When_noMatchingArchiveSessionEventIsAvailable() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(99L);
    Map<Long, String> archiveDates = givenArchiveDateBySession();

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(archiveDates)
                .deleteDateByUser(Map.of())
                .messageStatsByUser(Map.of())
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());

    // then
    assertThat(result.getEndDate(), is(nullValue()));
  }

  @Test
  void convertStatisticsEvent_Should_setLastActivityDate_When_userHasInteractions() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(1L);

    Instant messageDate = Instant.parse("2021-05-08T10:30:20Z");
    Instant videoCallDate = Instant.parse("2021-05-25T15:00:00Z"); // MAX!
    Instant bookingDate = Instant.parse("2021-05-10T12:00:00Z");

    Map<String, UserEventStats> messageStats = Map.of(ASKER_ID, new UserEventStats(3, messageDate));
    Map<String, UserEventStats> videoCallStats =
        Map.of(ASKER_ID, new UserEventStats(2, videoCallDate));
    Map<String, UserEventStats> bookingStats = Map.of(ASKER_ID, new UserEventStats(1, bookingDate));

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(Map.of())
                .deleteDateByUser(Map.of())
                .messageStatsByUser(messageStats)
                .videoCallStatsByUser(videoCallStats)
                .bookingStatsByUser(bookingStats)
                .build());

    // then
    assertThat(result.getLastActivityDate(), is("2021-05-25T15:00:00Z"));
  }

  @Test
  void convertStatisticsEvent_Should_setLastActivityDateToNull_When_userHasNoInteractions() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(1L);

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(Map.of())
                .deleteDateByUser(Map.of())
                .messageStatsByUser(Map.of())
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());

    // then
    assertThat(result.getLastActivityDate(), is(nullValue()));
  }

  @Test
  void convertStatisticsEvent_Should_setLastActivityDate_When_onlyOneInteractionTypeExists() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(1L);

    Instant messageDate = Instant.parse("2021-05-08T10:30:20Z");

    Map<String, UserEventStats> messageStats = Map.of(ASKER_ID, new UserEventStats(5, messageDate));

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(Map.of())
                .deleteDateByUser(Map.of())
                .messageStatsByUser(messageStats)
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());

    // then
    assertThat(result.getLastActivityDate(), is("2021-05-08T10:30:20Z"));
  }

  @Test
  void convertStatisticsEvent_Should_setLastActivityDate_When_otherUserHasInteractions() {
    // given
    StatisticsEvent registrationEvent = givenValidRegistrationStatisticEvent(1L);

    Map<String, UserEventStats> messageStats =
        Map.of("otherUserId", new UserEventStats(3, Instant.parse("2021-05-08T10:30:20Z")));

    // when
    RegistrationStatisticsResponseDTO result =
        registrationStatisticsDTOConverter.convertStatisticsEvent(
            registrationEvent,
            StatisticContainer.builder()
                .archiveDateBySession(Map.of())
                .deleteDateByUser(Map.of())
                .messageStatsByUser(messageStats)
                .videoCallStatsByUser(Map.of())
                .bookingStatsByUser(Map.of())
                .build());

    // then
    assertThat(result.getLastActivityDate(), is(nullValue())); // Kein Match für ASKER_ID
  }

  private StatisticsEvent givenValidRegistrationStatisticEvent(Long sessionId) {
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
            .tenantName("tenantName")
            .agencyName("agencyName")
            .referer("aReferer")
            .build();
    return StatisticsEvent.builder()
        .sessionId(sessionId)
        .eventType(EventType.REGISTRATION)
        .user(User.builder().userRole(UserRole.ASKER).id(ASKER_ID).build())
        .consultingType(ConsultingType.builder().id(CONSULTING_TYPE_ID).build())
        .agency(Agency.builder().id(AGENCY_ID).build())
        .timestamp(Instant.now())
        .metaData(metaData)
        .build();
  }

  private Map<Long, String> givenArchiveDateBySession() {
    return Map.of(
        1L, "2 end date for session 1",
        2L, "end date for session 2",
        999L, "dummy end date");
  }

  private Map<String, String> givenDeleteDateByUser() {
    return Map.of(ASKER_ID, "delete date for user 1", "user 2", "delete date for user 2");
  }
}
