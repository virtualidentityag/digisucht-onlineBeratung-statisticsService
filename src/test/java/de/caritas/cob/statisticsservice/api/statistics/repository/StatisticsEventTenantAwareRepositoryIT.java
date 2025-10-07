package de.caritas.cob.statisticsservice.api.statistics.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.caritas.cob.statisticsservice.StatisticsServiceApplication;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
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
    assertThat(allRegistrationStatistics, hasSize(1));
  }

  @Test
  public void
      getMessageCountsByReceiver_Should_ReturnMessageCountsGroupedByUserFilteredByTenantId() {
    Map<String, Integer> messageCountsByUser =
        statisticsEventTenantAwareRepository.getMessageCountsByUser(1L);

    org.assertj.core.api.Assertions.assertThat(messageCountsByUser)
        .hasSize(1)
        .containsEntry("10", 3);
  }

  @Test
  public void
      getVideoCallCountsByUser_Should_ReturnVideoCallCountsGroupedByUserFilteredByTenantId() {
    Map<String, Integer> videoCallCountsByUser =
        statisticsEventTenantAwareRepository.getVideoCallCountsByUser(1L);

    org.assertj.core.api.Assertions.assertThat(videoCallCountsByUser)
        .hasSize(1)
        .containsEntry("10", 2);
  }

  @Test
  public void getBookingCountsByUser_Should_ReturnBookingCountsGroupedByUserFilteredByTenantId() {
    Map<String, Integer> bookingCountsByUser =
        statisticsEventTenantAwareRepository.getBookingCountsByUser(1L);

    org.assertj.core.api.Assertions.assertThat(bookingCountsByUser)
        .hasSize(1)
        .containsEntry("10", 2);
  }

  @Test
  public void getDeleteDateByUser_Should_ReturnDeleteDateByUserFilteredByTenantId() {
    Map<String, String> deleteDateByUser =
        statisticsEventTenantAwareRepository.getDeleteDateByUser(1L);

    org.assertj.core.api.Assertions.assertThat(deleteDateByUser)
        .hasSize(2)
        .containsEntry("10", "2025-10-10T07:09:56Z")
        .containsEntry("11", "2025-10-11T07:09:56Z");
  }

  @Test
  public void
      getLatestArchiveSessionEventForSession_Should_ReturnLatestArchiveSessionEventForUserFilteredByTenantId() {
    Map<Long, String> result =
        statisticsEventTenantAwareRepository.getLatestArchiveDateBySession(1L);

    org.assertj.core.api.Assertions.assertThat(result)
        .hasSize(1)
        // the latest of the existing events
        .containsEntry(633L, "2022-10-18T10:00:00.00Z");
  }
}
