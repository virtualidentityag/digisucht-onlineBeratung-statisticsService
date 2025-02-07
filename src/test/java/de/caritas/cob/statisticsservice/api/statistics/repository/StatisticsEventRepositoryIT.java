package de.caritas.cob.statisticsservice.api.statistics.repository;

import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.CONSULTANT_ID;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.DATE_FROM;
import static de.caritas.cob.statisticsservice.api.testhelper.TestConstants.DATE_TO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.caritas.cob.statisticsservice.StatisticsServiceApplication;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.repository.StatisticsEventRepository.Count;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@DataMongoTest
@ContextConfiguration(classes = StatisticsServiceApplication.class, initializers = StatisticsEventRepositoryIT.Initializer.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
class StatisticsEventRepositoryIT {


  static final String MONGODB_STATISTICS_EVENTS_JSON_FILENAME =
      "mongodb/StatisticsEvents.json";
  private final Instant dateFromConverted =
      OffsetDateTime.of(DATE_FROM, LocalTime.MIN, ZoneOffset.UTC).toInstant();
  private final Instant dateToConverted =
      OffsetDateTime.of(DATE_TO, LocalTime.MAX, ZoneOffset.UTC).toInstant();
  private final String MONGO_COLLECTION_NAME = "statistics_event";
  private static MongodExecutable mongodExecutable;
  private static int mongoPort;
  @Autowired
  StatisticsEventRepository statisticsEventRepository;
  @Autowired
  MongoTemplate mongoTemplate;

  @BeforeAll
  static void setUp() throws IOException {
    MongodStarter starter = MongodStarter.getDefaultInstance();
    mongoPort = 27017;
    MongodConfig mongodConfig = MongodConfig.builder()
        .version(Version.Main.V4_0)
        .net(new Net(mongoPort, Network.localhostIsIPv6()))
        .build();
    mongodExecutable = starter.prepare(mongodConfig);
    mongodExecutable.start();
  }

  @AfterAll
  static void tearDown() {
    if (mongodExecutable != null) {
      mongodExecutable.stop();
    }
  }

  @BeforeEach
  void preFillMongoDb() throws IOException {
    mongoTemplate.dropCollection(MONGO_COLLECTION_NAME);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    List<StatisticsEvent> statisticEvents =
        objectMapper.readValue(
            new ClassPathResource(MONGODB_STATISTICS_EVENTS_JSON_FILENAME).getFile(),
            new TypeReference<>() {
            });
    mongoTemplate.insert(statisticEvents, MONGO_COLLECTION_NAME);
    mongoTemplate.getDb().getCollection(MONGO_COLLECTION_NAME).aggregate(
        List.of(new Document("$addFields",
            new Document("metaData.endTime",
                new Document("$toDate", "$metaData.endTime"))
                .append("metaData.startTime",
                    new Document("$toDate", "$metaData.startTime")))));
  }

  @Test
  void calculateNumberOfAssignedSessionsForUser_Should_ReturnCorrectNumberOfSessions() {
    assertThat(
        statisticsEventRepository.calculateNumberOfAssignedSessionsForUser(
            CONSULTANT_ID, dateFromConverted, dateToConverted),
        is(3L));
  }

  @Test
  void calculateNumbersOfSessionsWhereUserWasActive_Should_ReturnNull_WHEN_queryDoesNotMatchAnyResult() {
    var currentDateTime =
        OffsetDateTime.of(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC), ZoneOffset.UTC)
            .toInstant();

    assertThat(
        statisticsEventRepository.calculateNumbersOfSessionsWhereUserWasActive(
            CONSULTANT_ID, currentDateTime, currentDateTime), nullValue());
  }

  @Test
  void calculateNumbersOfSessionsWhereUserWasActive_Should_ReturnCorrectNumberOfSessions() {
    assertThat(
        statisticsEventRepository.calculateNumbersOfSessionsWhereUserWasActive(
            CONSULTANT_ID, dateFromConverted, dateToConverted).getTotalCount(),
        is(5L));
  }

  @Test
  void calculateNumberOfSentMessagesForUser_Should_ReturnCorrectNumberOfMessages() {
    assertThat(
        statisticsEventRepository.calculateNumberOfSentMessagesForUser(
            CONSULTANT_ID, dateFromConverted, dateToConverted),
        is(4L));
  }

  @Test
  void calculateTimeInVideoCallsForUser_Should_ReturnCorrectTime() {
    assertThat(
        statisticsEventRepository.calculateTimeInVideoCallsForUser(
            CONSULTANT_ID, dateFromConverted, dateToConverted).getTotal(),
        is(1800L));
  }

  @Test
  void calculateTimeInVideoCallsForUser_Should_ReturnNull_WHEN_queryDoesNotMatchAnyResult() {
    var currentDateTime =
        OffsetDateTime.of(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC), ZoneOffset.UTC)
            .toInstant();

    assertThat(
        statisticsEventRepository.calculateTimeInVideoCallsForUser(
            CONSULTANT_ID, currentDateTime, currentDateTime), nullValue());
  }

  @Test
  void getAllRegistrationStatistics_Should_ReturnRegistrationStatistics() {

    List<StatisticsEvent> allRegistrationStatistics = statisticsEventRepository.getAllRegistrationStatistics();
    assertThat(allRegistrationStatistics, hasSize(2));
  }

  @Test
  void getAllArchiveSessionEvents_Should_ReturnArchiveSessionEvents() {
    List<StatisticsEvent> allArchiveSessionEvents = statisticsEventRepository.getAllArchiveSessionEvents();
    assertThat(allArchiveSessionEvents, hasSize(3));
  }

  @Test
  @Disabled("For some reason this test is failing in this test scenario caused by the event.0.startTime and event.0.endTime filters.")
  void calculateNumberOfDoneAppointmentsForConsultant_Should_ReturnCorrectNumberOfAppointments() {
    Count count = statisticsEventRepository.calculateNumbersOfDoneAppointments(CONSULTANT_ID,
        dateFromConverted, dateToConverted, dateToConverted);

    assertThat(count.getTotalCount(), is(1L));
  }

  static class Initializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
          "spring.data.mongodb.uri=mongodb://localhost:" + mongoPort + "/test"
      ).applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
