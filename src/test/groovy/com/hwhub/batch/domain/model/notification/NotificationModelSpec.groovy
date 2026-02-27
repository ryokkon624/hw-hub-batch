package com.hwhub.batch.domain.model.notification

import com.hwhub.batch.domain.enums.NotificationLinkType
import com.hwhub.batch.domain.enums.NotificationType
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime

class NotificationModelSpec extends Specification {

    def "reconstructメソッドで適切なフィールドを持つNotificationModelが生成できること"() {
        given: "パラメータを準備"
        Long notificationId = 1L
        Long householdId = 10L
        String notificationType = NotificationType.TASK_ASSIGNED.getCode()
        Long actorUserId = 100L
        Long targetUserId = 200L
        boolean isRead = true
        LocalDateTime readAt = LocalDateTime.of(2023, 1, 1, 12, 0)
        String titleKey = "title.key"
        String bodyKey = "body.key"
        Map<String, Object> params = ["key": "value"]
        String linkType = NotificationLinkType.MY_TASKS.getCode()
        Long linkId = 50L
        LocalDateTime occurredAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        String aggregatedKey = "key_1"
        int aggregatedCount = 3

        when: "reconstructメソッドを呼び出す"
        NotificationModel model = NotificationModel.reconstruct(
                notificationId, householdId, notificationType, actorUserId, targetUserId,
                isRead, readAt, titleKey, bodyKey, params, linkType, linkId,
                occurredAt, aggregatedKey, aggregatedCount
        )

        then: "期待通りのフィールドが設定されていること"
        model.getNotificationId().value() == notificationId
        model.getHouseholdId() == householdId
        model.getNotificationType() == NotificationType.TASK_ASSIGNED
        model.getActorUserId() == actorUserId
        model.getTargetUserId() == targetUserId
        model.isRead() == isRead
        model.getReadAt() == readAt
        model.getMessage().titleKey() == titleKey
        model.getMessage().bodyKey() == bodyKey
        model.getMessage().params() == params
        model.getLink().linkType() == NotificationLinkType.MY_TASKS
        model.getLink().linkId() == linkId
        model.getOccurredAt() == occurredAt
        model.getAggregatedKey() == aggregatedKey
        model.getAggregatedCount() == aggregatedCount
    }

    def "reconstructFromEventメソッドでイベントパラメータからNotificationModelが生成できること"() {
        given: "イベントからのパラメータを準備"
        Long householdId = 10L
        String notificationType = NotificationType.TASK_ASSIGNED.getCode()
        Long actorUserId = 100L
        Long targetUserId = 200L
        LocalDateTime occurredAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        int aggregatedCount = 2

        when: "reconstructFromEventを呼び出す"
        NotificationModel model = NotificationModel.reconstructFromEvent(
                householdId, notificationType, actorUserId, targetUserId, occurredAt, aggregatedCount
        )

        then: "期待通りのフィールドが設定されること"
        model.getNotificationId() == null
        model.getHouseholdId() == householdId
        model.getNotificationType() == NotificationType.TASK_ASSIGNED
        model.getActorUserId() == actorUserId
        model.getTargetUserId() == targetUserId
        model.getOccurredAt() == occurredAt
        model.getAggregatedCount() == aggregatedCount
        !model.isRead()
        model.getReadAt() == null
        model.getMessage() == null
        model.getLink() == null
        model.getAggregatedKey() == null
    }

    def "newUnreadメソッドで未読のNotificationModelを新規作成できること"() {
        given: "パラメータを準備"
        Long householdId = 10L
        NotificationType type = NotificationType.TASK_ASSIGNED
        Long actorUserId = 100L
        Long targetUserId = 200L
        NotificationMessage message = new NotificationMessage("title", "body", [:])
        NotificationLink link = new NotificationLink(NotificationLinkType.MY_TASKS, null)
        LocalDateTime occurredAt = LocalDateTime.of(2023, 1, 1, 10, 0)

        when: "newUnreadを呼び出す"
        NotificationModel model = NotificationModel.newUnread(
                householdId, type, actorUserId, targetUserId, message, link, occurredAt
        )

        then: "期待通りのフィールドが設定されること"
        model.getNotificationId() == null
        model.getHouseholdId() == householdId
        model.getNotificationType() == type
        model.getActorUserId() == actorUserId
        model.getTargetUserId() == targetUserId
        model.getOccurredAt() == occurredAt
        model.getAggregatedCount() == 1
        !model.isRead()
        model.getReadAt() == null
        model.getMessage() == message
        model.getLink() == link
    }

    def "markReadメソッドで既読状態と既読日時が更新されること"() {
        given: "未読モデルを準備"
        NotificationModel model = NotificationModel.newUnread(
                10L, NotificationType.TASK_ASSIGNED, 100L, 200L,
                new NotificationMessage("t", "b", [:]),
                new NotificationLink(NotificationLinkType.MY_TASKS, null),
                LocalDateTime.now()
        )
        LocalDateTime readAt = LocalDateTime.of(2023, 1, 2, 10, 0)

        when: "markReadを呼び出す"
        model.markRead(readAt)

        then: "既読状態になること"
        model.isRead()
        model.getReadAt() == readAt
    }

    def "markReadメソッドでreadAtがnullの場合に例外がスローされること"() {
        given: "未読モデルを準備"
        NotificationModel model = NotificationModel.newUnread(
                10L, NotificationType.TASK_ASSIGNED, 100L, 200L,
                new NotificationMessage("t", "b", [:]),
                new NotificationLink(NotificationLinkType.MY_TASKS, null),
                LocalDateTime.now()
        )

        when: "nullでmarkReadを呼び出す"
        model.markRead(null)

        then: "例外が発生すること"
        thrown(NullPointerException)
    }

    def "setMessageAndLinkメソッドでメッセージとリンクが更新されること"() {
        given: "メッセージとリンクなしのモデルを準備"
        NotificationModel model = NotificationModel.reconstructFromEvent(
                10L, "0301", 100L, 200L, LocalDateTime.now(), 2
        )
        NotificationMessage newMessage = new NotificationMessage("new.title", "new.body", [:])
        NotificationLink newLink = new NotificationLink(NotificationLinkType.HOUSEHOLD, 1L)

        when: "setMessageAndLinkを呼び出す"
        model.setMessageAndLink(newMessage, newLink)

        then: "メッセージとリンクが更新されること"
        model.getMessage() == newMessage
        model.getLink() == newLink
    }

    @Unroll
    def "異常系: 不正な入力パラメータの場合、コンストラクタ経由でIllegalArgumentExceptionがスローされること (targetUserId=#targetUserId, isRead=#isRead, readAt=#readAt, aggregatedCount=#count, type=#type, occurredAt=#occurredAt)"() {
        when: "不正なパラメータでコンストラクタ経由（reconstructを利用）でインスタンス化を試みる"
        NotificationModel.reconstruct(
                1L, 10L, type, 100L, targetUserId, isRead, readAt, "title", "body", [:],
                NotificationLinkType.MY_TASKS.getCode(), 1L, occurredAt, "key", count
        )

        then: "例外が発生すること"
        thrown(resultException)

        where:
        type                                     | targetUserId | isRead | readAt           | count | occurredAt          || resultException
        NotificationType.TASK_ASSIGNED.getCode() | 0L           | false  | null             | 1     | LocalDateTime.now() || IllegalArgumentException
        NotificationType.TASK_ASSIGNED.getCode() | -1L          | false  | null             | 1     | LocalDateTime.now() || IllegalArgumentException
        NotificationType.TASK_ASSIGNED.getCode() | 200L         | true   | null             | 1     | LocalDateTime.now() || IllegalArgumentException
        NotificationType.TASK_ASSIGNED.getCode() | 200L         | false  | LocalDateTime.now() | 1     | LocalDateTime.now() || IllegalArgumentException
        NotificationType.TASK_ASSIGNED.getCode() | 200L         | false  | null             | 0     | LocalDateTime.now() || IllegalArgumentException
        NotificationType.TASK_ASSIGNED.getCode() | 200L         | false  | null             | -1    | LocalDateTime.now() || IllegalArgumentException
        NotificationType.TASK_ASSIGNED.getCode() | 200L         | false  | null             | 1     | null                || NullPointerException
    }
}
