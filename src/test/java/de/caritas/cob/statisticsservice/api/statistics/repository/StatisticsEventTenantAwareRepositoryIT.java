package de.caritas.cob.statisticsservice.api.statistics.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.caritas.cob.statisticsservice.StatisticsServiceApplication;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

@DataMongoTest()
@ContextConfiguration(classes = StatisticsServiceApplication.class, initializers = StatisticsEventTenantAwareRepositoryIT.Initializer.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@TestPropertySource(properties = "multitenancy.enabled=true")
class StatisticsEventTenantAwareRepositoryIT {

  static final String MONGODB_STATISTICS_EVENTS_JSON_FILENAME = "mongodb/StatisticsEvents.json";
  private final String MONGO_COLLECTION_NAME = "statistics_event";
  private static MongodExecutable mongodExecutable;
  private static int mongoPort;

  @Autowired
  StatisticsEventTenantAwareRepository statisticsEventTenantAwareRepository;

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
  }

  @Test
  void getAllRegistrationStatistics_Should_ReturnRegistrationStatisticsFilteredByTenantId() {

    List<StatisticsEvent> allRegistrationStatistics = statisticsEventTenantAwareRepository.getAllRegistrationStatistics(1L);
    assertThat(allRegistrationStatistics, hasSize(1));
  }

  @Test
  void getAllArchiveSessionEvents_Should_ReturnArchiveSessionEventsFilteredByTenantId() {
    List<StatisticsEvent> allArchiveSessionEvents = statisticsEventTenantAwareRepository.getAllArchiveSessionEvents(1L);
    assertThat(allArchiveSessionEvents, hasSize(2));
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
