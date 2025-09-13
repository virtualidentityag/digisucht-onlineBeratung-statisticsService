package de.caritas.cob.statisticsservice.api.helper;

import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.AGENCY_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.ASKER_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.CONSULTING_TYPE_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.Lists;
import de.caritas.cob.statisticsservice.api.model.EventType;
import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsResponseDTO;
import de.caritas.cob.statisticsservice.api.model.UserRole;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.Agency;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.ConsultingType;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticEventsContainer;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.User;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.ArchiveMetaData;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.DeleteAccountMetaData;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.RegistrationMetaData;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationStatisticsDTOConverterTest {

  @InjectMocks
  RegistrationStatisticsDTOConverter registrationStatisticsDTOConverter;

  private StatisticsEvent testEvent;

  private List<StatisticsEvent> archiveSessionEvents;

  private List<StatisticsEvent> deleteAccountEvents;

  @AfterEach
  void teardownEach() {
    testEvent = null;
    archiveSessionEvents = null;
  }

  @Test
  void convertStatisticsEvent_Should_convertToRegistrationStatisticResponse() {
    // given
    givenValidStatisticEvent(1L);

    // when
    RegistrationStatisticsResponseDTO result = registrationStatisticsDTOConverter.convertStatisticsEvent(
        testEvent, new StatisticEventsContainer());

    // then
    assertThat(result.getUserId(), is(ASKER_ID));
    assertThat(result.getRegistrationDate(),
        is("2022-09-15T09:14:45Z"));
    assertThat(result.getAge(), is(26));
    assertThat(result.getGender(), is("FEMALE"));
    assertThat(result.getMainTopicInternalAttribute(),
        is("alk"));
    assertThat(result.getTopicsInternalAttributes(),
        is(List.of("alk", "drogen")));
    assertThat(result.getPostalCode(), is("12345"));
    assertThat(result.getCounsellingRelation(),
        is("SELF_COUNSELLING"));
    assertThat(result.getTenantName(),
        is("tenantName"));
    assertThat(result.getAgencyName(),
        is("agencyName"));
    assertThat(result.getReferer(),
        is("aReferer"));
  }

  @Test
  void convertStatisticsEvent_Should_convertToRegistrationStatisticResponse_SkipCounting() {
    // given
    givenValidStatisticEvent(1L);

    var eventWithInvalidMetadataType = StatisticsEvent.builder()
        .sessionId(1L)
        .eventType(EventType.REGISTRATION)
        .user(User.builder().userRole(UserRole.ASKER).id(ASKER_ID).build())
        .consultingType(ConsultingType.builder().id(CONSULTING_TYPE_ID).build())
        .agency(Agency.builder().id(AGENCY_ID).build())
        .timestamp(Instant.now())
        .metaData(new LinkedHashMap<>())
        .build();

    var statisticEventsContainer = StatisticEventsContainer.builder()
        .videoCallStartedEvents(Lists.newArrayList(eventWithInvalidMetadataType))
        .bookingCreatedEvents(Lists.newArrayList(eventWithInvalidMetadataType))
        .consultantMessageCreatedEvents(Lists.newArrayList(eventWithInvalidMetadataType)).build();
    // when
    RegistrationStatisticsResponseDTO result = registrationStatisticsDTOConverter.convertStatisticsEvent(
        testEvent, statisticEventsContainer);

    // then
    assertThat(result.getUserId(), is(ASKER_ID));
    assertThat(result.getRegistrationDate(),
        is("2022-09-15T09:14:45Z"));
    assertThat(result.getAge(), is(26));
    assertThat(result.getGender(), is("FEMALE"));
    assertThat(result.getMainTopicInternalAttribute(),
        is("alk"));
    assertThat(result.getTopicsInternalAttributes(),
        is(List.of("alk", "drogen")));
    assertThat(result.getPostalCode(), is("12345"));
    assertThat(result.getCounsellingRelation(),
        is("SELF_COUNSELLING"));
    assertThat(result.getTenantName(),
        is("tenantName"));
    assertThat(result.getAgencyName(),
        is("agencyName"));
    assertThat(result.getReferer(),
        is("aReferer"));
  }


  @Test
  void convertStatisticsEvent_Should_notFail_When_archiveSessionEventsAreNull() {
    // given
    givenValidStatisticEvent(1L);

    // when
    RegistrationStatisticsResponseDTO result = registrationStatisticsDTOConverter.convertStatisticsEvent(
        testEvent, new StatisticEventsContainer());

    // then
    assertThat(result.getEndDate(), is(nullValue()));
  }

  @Test
  void convertStatisticsEvent_Should_addNewestArchiveSessionEndDate_When_multipleArchiveSessionEventsAreAvailable() {
    // given
    givenValidStatisticEvent(1L);
    givenValidArchiveStatisticEvents();

    // when
    RegistrationStatisticsResponseDTO result = registrationStatisticsDTOConverter.convertStatisticsEvent(
        testEvent, StatisticEventsContainer.builder()
            .archiveSessionEvents(archiveSessionEvents).build());

    // then
    assertThat(result.getEndDate(), is("2 end date for session 1"));
  }

  @Test
  void convertStatisticsEvent_Should_takeDeleteDateAsSessionEndDate_When_multipleArchiveSessionEventsAreAvailableAndDeleteDateExists() {
    // given
    givenValidStatisticEvent(1L);
    givenValidArchiveStatisticEvents();
    givenAccountDeleteStatisticEvents();

    // when
    RegistrationStatisticsResponseDTO result = registrationStatisticsDTOConverter.convertStatisticsEvent(
        testEvent, StatisticEventsContainer.builder()
            .archiveSessionEvents(archiveSessionEvents)
            .deleteAccountEvents(deleteAccountEvents)
            .build());

    // then
    assertThat(result.getEndDate(), is("delete date for user 1"));
  }

  @Test
  void convertStatisticsEvent_Should_addArchiveSessionEndDate_When_onlyOneArchiveSessionEventIsAvailable() {
    // given
    givenValidStatisticEvent(2L);
    givenValidArchiveStatisticEvents();

    // when
    RegistrationStatisticsResponseDTO result = registrationStatisticsDTOConverter.convertStatisticsEvent(
        testEvent, StatisticEventsContainer.builder()
            .archiveSessionEvents(archiveSessionEvents)
            .build());

    // then
    assertThat(result.getEndDate(), is("end date for session 2"));
  }

  @Test
  void convertStatisticsEvent_Should_notAddArchiveSessionEndDate_When_noMatchingArchiveSessionEventIsAvailable() {
    // given
    givenValidStatisticEvent(99L);
    givenValidArchiveStatisticEvents();

    // when
    RegistrationStatisticsResponseDTO result = registrationStatisticsDTOConverter.convertStatisticsEvent(
        testEvent, StatisticEventsContainer.builder()
            .archiveSessionEvents(archiveSessionEvents)
            .build());

    // then
    assertThat(result.getEndDate(), is(nullValue()));
  }

  private void givenValidStatisticEvent(Long sessionId) {
    Object metaData = RegistrationMetaData.builder()
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
    testEvent = StatisticsEvent.builder()
        .sessionId(sessionId)
        .eventType(EventType.REGISTRATION)
        .user(User.builder().userRole(UserRole.ASKER).id(ASKER_ID).build())
        .consultingType(ConsultingType.builder().id(CONSULTING_TYPE_ID).build())
        .agency(Agency.builder().id(AGENCY_ID).build())
        .timestamp(Instant.now())
        .metaData(metaData)
        .build();
  }

  private void givenAccountDeleteStatisticEvents() {
    deleteAccountEvents = List.of(
        deleteEvent(ASKER_ID, "2022-10-19T10:00:00.00Z", "delete date for user 1"),
        deleteEvent("user 2", "2022-10-17T10:00:00.00Z", "delete date for user 2"));
  }


  private void givenValidArchiveStatisticEvents() {
    archiveSessionEvents = List.of(
        archiveEvent(1L, "2022-10-17T10:00:00.00Z", "1 end date for session 1"),
        archiveEvent(1L, "2022-10-18T10:00:00.00Z", "2 end date for session 1"),
        archiveEvent(2L, "2022-10-18T10:00:00.00Z", "end date for session 2"),
        archiveEvent(999L, "2022-10-19T10:00:00.00Z", "dummy end date"));
  }

  private StatisticsEvent archiveEvent(Long sessionId, String timestampString, String endDate) {
    Object metaData = ArchiveMetaData.builder().endDate(endDate).build();
    return StatisticsEvent.builder().timestamp(Instant.parse(timestampString)).sessionId(sessionId)
        .metaData(metaData).build();
  }

  private StatisticsEvent deleteEvent(String userId, String timestampString, String deleteDate) {
    Object metaData = DeleteAccountMetaData.builder().deleteDate(deleteDate).build();
    User user = new User();
    user.setId(userId);
    return StatisticsEvent.builder().timestamp(Instant.parse(timestampString)).user(user)
        .metaData(metaData).build();
  }
}
