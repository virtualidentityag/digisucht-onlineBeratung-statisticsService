package de.caritas.cob.statisticsservice.api.statistics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.caritas.cob.statisticsservice.StatisticsServiceApplication;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventStats;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@DataMongoTest()
@ContextConfiguration(classes = StatisticsServiceApplication.class)
@RunWith(SpringRunner.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@TestPropertySource(properties = "multitenancy.enabled=true")
public class StatisticsEventTenantAwareRepositoryIT {

  public static final String MONGODB_STATISTICS_EVENTS_JSON_FILENAME =
      "mongodb/StatisticsEvents.json";
  private static final String MONGO_COLLECTION_NAME = "statistics_event";

  @Autowired StatisticsEventTenantAwareRepository statisticsEventTenantAwareRepository;
  @Autowired MongoTemplate mongoTemplate;

  @Before
  public void preFillMongoDb() throws IOException {
    mongoTemplate.dropCollection(MONGO_COLLECTION_NAME);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    List<StatisticsEvent> statisticEvents =
        objectMapper.readValue(
            new ClassPathResource(MONGODB_STATISTICS_EVENTS_JSON_FILENAME).getFile(),
            new TypeReference<>() {});
    mongoTemplate.insert(statisticEvents, MONGO_COLLECTION_NAME);
  }

  @Test
  public void getAllRegistrationStatistics_Should_ReturnRegistrationStatisticsFilteredByTenantId() {
    List<StatisticsEvent> allRegistrationStatistics =
        statisticsEventTenantAwareRepository.getAllRegistrationStatistics(1L);
    assertThat(allRegistrationStatistics).hasSize(1);
  }

  @Test
  public void getMessageStatsByUser_Should_ReturnStatsGroupedByUserFilteredByTenantId() {
    Map<String, UserEventStats> messageStatsByUser =
        statisticsEventTenantAwareRepository.getMessageStatsByUser(1L);

    assertThat(messageStatsByUser)
        .hasSize(1)
        .satisfies(
            map -> {
              assertThat(map.get("10").getCount()).isEqualTo(3);
              assertThat(map.get("10").getLastInteraction()).isEqualTo("2021-05-08T10:30:20.294Z");
            });
  }

  @Test
  public void getVideoCallStatsByUser_Should_ReturnStatsGroupedByUserFilteredByTenantId() {
    Map<String, UserEventStats> videoCallStatsByUser =
        statisticsEventTenantAwareRepository.getVideoCallStatsByUser(1L);

    assertThat(videoCallStatsByUser)
        .hasSize(1)
        .satisfies(
            map -> {
              // Nur User 10 (tenantId=1), neuester Call: 2021-05-25
              assertThat(map.get("10").getCount()).isEqualTo(2);
              assertThat(map.get("10").getLastInteraction()).isEqualTo("2021-05-25T15:00:00.00Z");
            });
  }

  @Test
  public void getBookingStatsByUser_Should_ReturnStatsGroupedByUserFilteredByTenantId() {
    Map<String, UserEventStats> bookingStatsByUser =
        statisticsEventTenantAwareRepository.getBookingStatsByUser(1L);

    assertThat(bookingStatsByUser)
        .hasSize(1)
        .satisfies(
            map -> {
              assertThat(map.get("10").getCount()).isEqualTo(2);
              assertThat(map.get("10").getLastInteraction()).isEqualTo("2021-05-10T15:00:00.000Z");
            });
  }

  @Test
  public void getDeleteDateByUser_Should_ReturnDeleteDateByUserFilteredByTenantId() {
    Map<String, String> deleteDateByUser =
        statisticsEventTenantAwareRepository.getDeleteDateByUser(1L);

    assertThat(deleteDateByUser)
        .hasSize(2)
        .containsEntry("10", "2025-10-10T07:09:56Z")
        .containsEntry("11", "2025-10-11T07:09:56Z");
  }

  @Test
  public void
      getLatestArchiveSessionEventForSession_Should_ReturnLatestArchiveSessionEventForUserFilteredByTenantId() {
    Map<Long, String> result =
        statisticsEventTenantAwareRepository.getLatestArchiveDateBySession(1L);

    assertThat(result)
        .hasSize(1)
        // the latest of the existing events
        .containsEntry(633L, "2022-10-18T10:00:00.00Z");
  }
}
