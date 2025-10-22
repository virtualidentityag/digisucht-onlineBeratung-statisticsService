package de.caritas.cob.statisticsservice.api.statistics.repository;

import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.SessionArchiveDate;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserDeleteDate;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventAggregation;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventStats;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface StatisticsEventTenantAwareRepository
    extends MongoRepository<StatisticsEvent, String> {

  /** Retrieves all registration statistics for a specific tenant. */
  @Query(value = "{'eventType': 'REGISTRATION', 'metaData.tenantId': ?0}")
  List<StatisticsEvent> getAllRegistrationStatistics(Long tenantId);

  /**
   * Aggregates message statistics (count and last interaction date) per advice seeker for a
   * specific tenant.
   */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'CREATE_MESSAGE', 'user.userRole': 'CONSULTANT', 'metaData.receiverId': { '$ne': null }, 'metaData.tenantId': ?0 } }",
        "{ '$group': { '_id': '$metaData.receiverId', 'count': { '$sum': 1 }, 'lastInteraction': { '$max': '$timestamp' } } }"
      })
  List<UserEventAggregation> aggregateMessageStatsByUser(Long tenantId);

  /** Returns message statistics as a map of advice seeker IDs to stats for a specific tenant. */
  default Map<String, UserEventStats> getMessageStatsByUser(Long tenantId) {
    return aggregateMessageStatsByUser(tenantId).stream()
        .collect(
            Collectors.toMap(
                UserEventAggregation::getId,
                event -> new UserEventStats(event.getCount(), event.getLastInteraction())));
  }

  /**
   * Aggregates video call statistics (count and last interaction date) per advice seeker for a
   * specific tenant.
   */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'START_VIDEO_CALL', 'metaData.adviceSeekerId': { '$ne': null }, 'metaData.tenantId': ?0 } }",
        "{ '$group': { '_id': '$metaData.adviceSeekerId', 'count': { '$sum': 1 }, 'lastInteraction': { '$max': '$timestamp' } } }"
      })
  List<UserEventAggregation> aggregateVideoCallStatsByUser(Long tenantId);

  /** Returns video call statistics as a map of advice seeker IDs to stats for a specific tenant. */
  default Map<String, UserEventStats> getVideoCallStatsByUser(Long tenantId) {
    return aggregateVideoCallStatsByUser(tenantId).stream()
        .collect(
            Collectors.toMap(
                UserEventAggregation::getId,
                event -> new UserEventStats(event.getCount(), event.getLastInteraction())));
  }

  /**
   * Aggregates booking statistics (count and last interaction date) per advice seeker for a
   * specific tenant.
   */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'BOOKING_CREATED', 'metaData.adviceSeekerId': { '$ne': null }, 'metaData.tenantId': ?0  } }",
        "{ '$group': { '_id': '$metaData.adviceSeekerId', 'count': { '$sum': 1 }, 'lastInteraction': { '$max': '$timestamp' }} }"
      })
  List<UserEventAggregation> aggregateBookingStatsByUser(Long tenantId);

  /** Returns booking statistics as a map of advice seeker IDs to stats for a specific tenant. */
  default Map<String, UserEventStats> getBookingStatsByUser(Long tenantId) {
    return aggregateBookingStatsByUser(tenantId).stream()
        .collect(
            Collectors.toMap(
                UserEventAggregation::getId,
                event -> new UserEventStats(event.getCount(), event.getLastInteraction())));
  }

  /** Aggregates the latest archive date per session for a specific tenant. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'ARCHIVE_SESSION', 'metaData.tenantId': ?0 } }",
        "{ '$sort': { 'timestamp': -1 } }",
        "{ '$group': { '_id': '$sessionId', 'endDate': { '$first': '$metaData.endDate' } } }"
      })
  List<SessionArchiveDate> aggregateLatestArchiveDateBySession(Long tenantId);

  /** Returns archive dates as a map of session IDs to end dates for a specific tenant. */
  default Map<Long, String> getLatestArchiveDateBySession(Long tenantId) {
    return aggregateLatestArchiveDateBySession(tenantId).stream()
        .collect(Collectors.toMap(SessionArchiveDate::getId, SessionArchiveDate::getEndDate));
  }

  /** Aggregates the account deletion date per user for a specific tenant. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'DELETE_ACCOUNT', 'metaData.tenantId': ?0 } }",
        "{ '$group': { '_id': '$user._id', 'deleteDate': { '$first': '$metaData.deleteDate' } } }"
      })
  List<UserDeleteDate> aggregateDeleteDateByUser(Long tenantId);

  /** Returns deletion dates as a map of user IDs to deletion dates for a specific tenant. */
  default Map<String, String> getDeleteDateByUser(Long tenantId) {
    return aggregateDeleteDateByUser(tenantId).stream()
        .collect(Collectors.toMap(UserDeleteDate::getId, UserDeleteDate::getDeleteDate));
  }
}
