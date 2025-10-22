package de.caritas.cob.statisticsservice.api.statistics.repository;

import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.SessionArchiveDate;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserDeleteDate;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventAggregation;
import de.caritas.cob.statisticsservice.api.statistics.repository.projection.UserEventStats;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface StatisticsEventRepository extends MongoRepository<StatisticsEvent, String> {

  @Data
  @AllArgsConstructor
  class Count {
    long totalCount;
  }

  @Data
  @AllArgsConstructor
  class Duration {
    long total;
  }

  /**
   * Calculate the number of sessions in which the user was active. Active means that the user has
   * either sent a message or initiated a video call.
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the number of sessions in which the user was active. Could also return null if the
   *     mongo query returns no results.
   */
  @Aggregation(
      pipeline = {
        "{$match:{'user._id': ?0,'sessionId': {$exists:true,$ne:null},'eventType': {'$in': ['START_VIDEO_CALL','CREATE_MESSAGE']},'timestamp':{$gte:?1,$lte:?2}}}",
        "{$group:{'_id': '$sessionId', 'count': { '$sum': 1 }}}",
        "{$project:{'_id': 0}}",
        "{$count:'totalCount'}"
      })
  Count calculateNumbersOfSessionsWhereUserWasActive(
      String userId, Instant dateFrom, Instant dateTo);

  /**
   * Calculate the number of sent messages in the given period.
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the number of sent messages in the given period
   */
  @Query(
      value = "{'user._id': ?0, 'eventType': 'CREATE_MESSAGE', 'timestamp':{$gte:?1,$lte:?2}}",
      count = true)
  long calculateNumberOfSentMessagesForUser(String userId, Instant dateFrom, Instant dateTo);

  /**
   * Calculate the time a user has spent in video calls in the given time period.
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the total time in seconds. Could also return null if the mongo query returns no
   *     results.
   */
  @Aggregation(
      pipeline = {
        "{'$match':{'user._id': ?0,'eventType': 'START_VIDEO_CALL','timestamp':{$gte:?1,$lte:?2}}}",
        "{'$group':{'_id':'','total':{'$sum':'$metaData.duration'}}}"
      })
  Duration calculateTimeInVideoCallsForUser(String userId, Instant dateFrom, Instant dateTo);

  /**
   * Calculate the number of sessions assigned to a user in the given time period.
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the number of the new sessions
   */
  @Query(
      value = "{'user._id': ?0, 'eventType': 'ASSIGN_SESSION', 'timestamp':{$gte:?1,$lte:?2}}",
      count = true)
  long calculateNumberOfAssignedSessionsForUser(String userId, Instant dateFrom, Instant dateTo);

  /**
   * Calculate the number of done appointments. Done mean that the endTime of the appointment or the
   * endTime of the latest reschedule has been reached, and it was not canceled
   *
   * @param userId the user id
   * @param dateFrom the start date of the period
   * @param dateTo the end date of the period
   * @return the number of done appointments. Could also return null if the mongo query returns no
   *     results.
   */
  @Aggregation(
      pipeline = {
        "{'$match': {$and: [{'metaData.currentBookingId': {$ne: null}}, {'user._id': {$eq:?0}}]}}",
        "{'$sort': {'timestamp': -1}}",
        "{'$group': {'_id': '$metaData.currentBookingId','events': {'$push': {'timestamp': '$timestamp','event': '$eventType','type': '$metaData.type','startTime': '$metaData.startTime','endTime': '$metaData.endTime'}}}}",
        "{'$match': {$and: [{'events.0.event': {'$ne': 'BOOKING_CANCELLED'}},  {'events.0.startTime': {$gte:?1}}, {'events.0.endTime': {$gte:?1}}, {'events.0.endTime': {$lte:?2}}, {'events.0.endTime': {$lte:?3}}]}}",
        "{'$count': 'totalCount'}"
      })
  Count calculateNumbersOfDoneAppointments(
      String userId, Instant dateFrom, Instant dateTo, Instant now);

  /** Retrieves all registration statistics events. */
  @Query(value = "{'eventType': 'REGISTRATION'}")
  List<StatisticsEvent> getAllRegistrationStatistics();

  /** Aggregates message statistics (count and last interaction date) per advice seeker. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'CREATE_MESSAGE', 'user.userRole': 'CONSULTANT', 'metaData.receiverId': { '$ne': null } } }",
        "{ '$group': { '_id': '$metaData.receiverId', 'count': { '$sum': 1 }, 'lastInteraction': { '$max': '$timestamp' } } }"
      })
  List<UserEventAggregation> aggregateMessageStatsByUser();

  /** Returns message statistics as a map of advice seeker IDs to stats. */
  default Map<String, UserEventStats> getMessageStatsByUser() {
    return aggregateMessageStatsByUser().stream()
        .collect(
            Collectors.toMap(
                UserEventAggregation::getId,
                event -> new UserEventStats(event.getCount(), event.getLastInteraction())));
  }

  /** Aggregates video call statistics (count and last interaction date) per advice seeker. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'START_VIDEO_CALL', 'metaData.adviceSeekerId': { '$ne': null } } }",
        "{ '$group': { '_id': '$metaData.adviceSeekerId', 'count': { '$sum': 1 }, 'lastInteraction': { '$max': '$timestamp' } } }"
      })
  List<UserEventAggregation> aggregateVideoCallStatsByUser();

  /** Returns video call statistics as a map of advice seeker IDs to stats. */
  default Map<String, UserEventStats> getVideoCallStatsByUser() {
    return aggregateVideoCallStatsByUser().stream()
        .collect(
            Collectors.toMap(
                UserEventAggregation::getId,
                event -> new UserEventStats(event.getCount(), event.getLastInteraction())));
  }

  /** Aggregates booking statistics (count and last interaction date) per advice seeker. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'BOOKING_CREATED', 'metaData.adviceSeekerId': { '$ne': null } } }",
        "{ '$group': { '_id': '$metaData.adviceSeekerId', 'count': { '$sum': 1 }, 'lastInteraction': { '$max': '$timestamp' } } }"
      })
  List<UserEventAggregation> aggregateBookingStatsByUser();

  /** Returns booking statistics as a map of advice seeker IDs to stats. */
  default Map<String, UserEventStats> getBookingStatsByUser() {
    return aggregateBookingStatsByUser().stream()
        .collect(
            Collectors.toMap(
                UserEventAggregation::getId,
                event -> new UserEventStats(event.getCount(), event.getLastInteraction())));
  }

  /** Aggregates the latest archive date per session. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'ARCHIVE_SESSION' } }",
        "{ '$sort': { 'timestamp': -1 } }",
        "{ '$group': {'_id': '$sessionId', 'endDate': { '$first': '$metaData.endDate' } } }"
      })
  List<SessionArchiveDate> aggregateLatestArchiveDateBySession();

  /** Returns archive dates as a map of session IDs to end dates. */
  default Map<Long, String> getLatestArchiveDateBySession() {
    return aggregateLatestArchiveDateBySession().stream()
        .collect(Collectors.toMap(SessionArchiveDate::getId, SessionArchiveDate::getEndDate));
  }

  /** Aggregates the account deletion date per user. */
  @Aggregation(
      pipeline = {
        "{ '$match': { 'eventType': 'DELETE_ACCOUNT' } }",
        "{ '$group': { '_id': '$user._id', 'deleteDate': { '$first': '$metaData.deleteDate' } } }"
      })
  List<UserDeleteDate> aggregateDeleteDateByUser();

  /** Returns deletion dates as a map of user IDs to deletion dates. */
  default Map<String, String> getDeleteDateByUser() {
    return aggregateDeleteDateByUser().stream()
        .collect(Collectors.toMap(UserDeleteDate::getId, UserDeleteDate::getDeleteDate));
  }
}
