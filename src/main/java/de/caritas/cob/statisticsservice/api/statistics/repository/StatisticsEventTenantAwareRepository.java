package de.caritas.cob.statisticsservice.api.statistics.repository;

import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.SessionArchiveDate;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.UserDeleteDate;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.UserEventCount;
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

  /** Aggregates the number of messages sent by consultants, grouped by advice seeker (receiver). */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'CREATE_MESSAGE', 'user.userRole': 'CONSULTANT', 'metaData.receiverId': { '$ne': null }, 'metaData.tenantId': ?0 } }",
        "{ '$group': { '_id': '$metaData.receiverId', 'count': { '$sum': 1 } } }"
      })
  List<UserEventCount> aggregateMessageCountsByUser(Long currentTenant);

  /** Converts the aggregated message counts into a map of advice seeker IDs to message counts. */
  default Map<String, Integer> getMessageCountsByUser(Long currentTenant) {
    return aggregateMessageCountsByUser(currentTenant).stream()
        .collect(Collectors.toMap(UserEventCount::getId, UserEventCount::getCount));
  }

  /** Aggregates the number of video calls started, grouped by advice seeker. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'START_VIDEO_CALL', 'metaData.adviceSeekerId': { '$ne': null }, 'metaData.tenantId': ?0 } }",
        "{ '$group': { '_id': '$metaData.adviceSeekerId', 'count': { '$sum': 1 } } }"
      })
  List<UserEventCount> aggregateVideoCallCountsByUser(Long currentTenant);

  /** Converts the aggregated video call counts into a map of advice seeker IDs to call counts. */
  default Map<String, Integer> getVideoCallCountsByUser(Long currentTenant) {
    return aggregateVideoCallCountsByUser(currentTenant).stream()
        .collect(Collectors.toMap(UserEventCount::getId, UserEventCount::getCount));
  }

  /** Aggregates the number of bookings created, grouped by advice seeker. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'BOOKING_CREATED', 'metaData.adviceSeekerId': { '$ne': null }, 'metaData.tenantId': ?0  } }",
        "{ '$group': { '_id': '$metaData.adviceSeekerId', 'count': { '$sum': 1 } } }"
      })
  List<UserEventCount> aggregateBookingCountsByUser(Long currentTenant);

  /** Converts the aggregated booking counts into a map of advice seeker IDs to booking counts. */
  default Map<String, Integer> getBookingCountsByUser(Long currentTenant) {
    return aggregateBookingCountsByUser(currentTenant).stream()
        .collect(Collectors.toMap(UserEventCount::getId, UserEventCount::getCount));
  }

  /** Aggregates the latest archive date per session for a specific tenant. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'ARCHIVE_SESSION', 'metaData.tenantId': ?0 } }",
        "{ '$sort': { 'timestamp': -1 } }",
        "{ '$group': { '_id': '$sessionId', 'endDate': { '$first': '$metaData.endDate' } } }"
      })
  List<SessionArchiveDate> aggregateLatestArchiveDateBySession(Long tenantId);

  /** Converts the aggregated archive dates into a map of session IDs to archive end dates. */
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

  /** Converts the aggregated deletion dates into a map of user IDs to deletion dates. */
  default Map<String, String> getDeleteDateByUser(Long tenantId) {
    return aggregateDeleteDateByUser(tenantId).stream()
        .collect(Collectors.toMap(UserDeleteDate::getId, UserDeleteDate::getDeleteDate));
  }
}
