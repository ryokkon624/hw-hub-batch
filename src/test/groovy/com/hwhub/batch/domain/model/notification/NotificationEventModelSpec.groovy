package com.hwhub.batch.domain.model.notification

import com.hwhub.batch.domain.enums.EventStatus
import com.hwhub.batch.domain.enums.NotificationType
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime

class NotificationEventModelSpec extends Specification {

    def "reconstructメソッドで適切なフィールドを持つNotificationEventModelが生成できること"() {
        given: "パラメータを準備"
        Long notificationEventId = 1L
        Long householdId = 10L
        String notificationType = NotificationType.TASK_ASSIGNED.getCode()
        Long actorUserId = 100L
        Long targetUserId = 200L
        Long entityId = 300L
        LocalDate aggregationDate = LocalDate.of(2023, 1, 1)
        LocalDateTime occurredAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        String eventStatus = EventStatus.DONE.getCode()
        String processingKey = "process-123"
        LocalDate processingStartedAt = LocalDate.of(2023, 1, 1)
        LocalDate processedAt = LocalDate.of(2023, 1, 2)

        when: "reconstructを呼び出す"
        NotificationEventModel model = NotificationEventModel.reconstruct(
                notificationEventId, householdId, notificationType, actorUserId, targetUserId,
                entityId, aggregationDate, occurredAt, eventStatus, processingKey,
                processingStartedAt, processedAt
        )

        then: "期待通りのフィールドが設定されること"
        model.getNotificationEventId().value() == notificationEventId
        model.getHouseholdId() == householdId
        model.getNotificationType() == NotificationType.TASK_ASSIGNED
        model.getActorUserId() == actorUserId
        model.getTargetUserId() == targetUserId
        model.getEntityId() == entityId
        model.getAggregationDate() == aggregationDate
        model.getOccurredAt() == occurredAt
        model.getEventStatus() == EventStatus.DONE
        model.getProcessingKey() == processingKey
        model.getProcessingStartedAt() == processingStartedAt
        model.getProcessedAt() == processedAt
    }
}
