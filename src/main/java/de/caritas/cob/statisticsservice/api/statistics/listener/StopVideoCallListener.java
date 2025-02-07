package de.caritas.cob.statisticsservice.api.statistics.listener;

import de.caritas.cob.statisticsservice.api.model.EventType;
import de.caritas.cob.statisticsservice.api.model.StopVideoCallStatisticsEventMessage;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.StartVideoCallMetaData;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.VideoCallStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/** AMQP Listener for stop video call message statistics event. */
@Service
@RequiredArgsConstructor
public class StopVideoCallListener {

  private final @NonNull MongoTemplate mongoTemplate;

  /**
   * Consumer for stop video call statics statistics event.
   *
   * @param eventMessage the {@link StopVideoCallStatisticsEventMessage} instance
   */
  @RabbitListener(
      id = "stop-video-call-event-listener",
      queues = "#{rabbitMqConfig.QUEUE_NAME_STOP_VIDEO_CALL}",
      containerFactory = "simpleRabbitListenerContainerFactory")
  public void receiveMessage(StopVideoCallStatisticsEventMessage eventMessage) {

    StatisticsEvent statisticsEvent = fetchVideoCallStartEvent(eventMessage);
    StartVideoCallMetaData metaData = (StartVideoCallMetaData) statisticsEvent.getMetaData();

    statisticsEvent.setMetaData(enrichMetaData(metaData, eventMessage, statisticsEvent));
    mongoTemplate.save(statisticsEvent);
  }

  private StatisticsEvent fetchVideoCallStartEvent(
      StopVideoCallStatisticsEventMessage eventMessage) {
    List<StatisticsEvent> statisticsEventList =
        mongoTemplate.find(buildQuery(eventMessage.getVideoCallUuid()), StatisticsEvent.class);

    checkIfListIsEmpty(eventMessage, statisticsEventList);
    checkForMultipleEvents(eventMessage, statisticsEventList);

    return statisticsEventList.get(0);
  }

  private Query buildQuery(String videoCallUuid) {
    return new Query().addCriteria(
            Criteria.where("metaData.videoCallUuid").is(videoCallUuid)
                    .and("metaData.status").is(VideoCallStatus.ONGOING.toString())
    );
  }

  private StartVideoCallMetaData enrichMetaData(
      StartVideoCallMetaData metaData,
      StopVideoCallStatisticsEventMessage eventMessage,
      StatisticsEvent statisticsEvent) {
    metaData.setDuration(calculateDuration(eventMessage, statisticsEvent));
    metaData.setTimestampStop(eventMessage.getTimestamp().toInstant());
    metaData.setStatus(VideoCallStatus.FINISHED);
    return metaData;
  }

  private long calculateDuration(
      StopVideoCallStatisticsEventMessage eventMessage, StatisticsEvent statisticsEvent) {
    return Duration.between(statisticsEvent.getTimestamp(), eventMessage.getTimestamp().toInstant())
        .getSeconds();
  }

  private void checkForMultipleEvents(
      StopVideoCallStatisticsEventMessage eventMessage, List<StatisticsEvent> statisticsEventList) {
    if (statisticsEventList.size() > 1) {
      throw new AmqpException(
          "More than one %s events for call uid %s found".formatted(
              EventType.START_VIDEO_CALL, eventMessage.getVideoCallUuid()));
    }
  }

  private void checkIfListIsEmpty(
      StopVideoCallStatisticsEventMessage eventMessage, List<StatisticsEvent> statisticsEventList) {
    if (statisticsEventList.isEmpty()) {
      throw new AmqpException(
          "No %s event for call uid %s found".formatted(
              EventType.START_VIDEO_CALL, eventMessage.getVideoCallUuid()));
    }
  }
}
