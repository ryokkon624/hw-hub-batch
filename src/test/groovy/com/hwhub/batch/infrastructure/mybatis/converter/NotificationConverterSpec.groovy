package com.hwhub.batch.infrastructure.mybatis.converter

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwhub.batch.domain.enums.NotificationLinkType
import com.hwhub.batch.domain.enums.NotificationType
import com.hwhub.batch.domain.model.notification.NotificationModel
import com.hwhub.batch.infrastructure.mybatis.custom.entity.NotificationEventAggregationRow
import com.hwhub.batch.infrastructure.mybatis.generated.entity.TNotification
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId

class NotificationConverterSpec extends Specification {

    ObjectMapper objectMapper
    NotificationConverter converter

    def setup() {
        objectMapper = Mock(ObjectMapper)
        converter = new NotificationConverter(objectMapper)
    }

    def "toEntityメソッドでモデルがnullの場合、nullが返却されること"() {
        expect:
        converter.toEntity(null) == null
    }

    def "toEntityメソッドでNotificationModelからTNotificationへマッピングされること"() {
        given: "通知モデルを準備"
        LocalDateTime occurredAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        LocalDateTime readAt = LocalDateTime.of(2023, 1, 2, 10, 0)
        Map<String, Object> params = ["key": "value"]
        
        NotificationModel model = NotificationModel.reconstruct(
                1L, 10L, "0301", 100L, 200L, true, readAt,
                "title", "body", params, "MyTasks", 50L, occurredAt, "agg_key", 5
        )

        and: "ObjectMapperのモック設定"
        objectMapper.writeValueAsString(params) >> '{"key":"value"}'

        when: "toEntityを呼び出す"
        TNotification entity = converter.toEntity(model)

        then: "期待通りのマッピングが行われること"
        entity.getNotificationId() == 1L
        entity.getHouseholdId() == 10L
        entity.getNotificationType() == "0301"
        entity.getActorUserId() == 100L
        entity.getTargetUserId() == 200L
        entity.getIsRead()
        entity.getReadAt() == java.util.Date.from(readAt.atZone(ZoneId.systemDefault()).toInstant())
        entity.getTitleKey() == "title"
        entity.getBodyKey() == "body"
        entity.getParamsJson() == '{"key":"value"}'
        entity.getLinkType() == "MyTasks"
        entity.getLinkId() == 50L
        entity.getOccurredAt() == java.util.Date.from(occurredAt.atZone(ZoneId.systemDefault()).toInstant())
        entity.getAggregatedKey() == "agg_key"
        entity.getAggregatedCount() == 5
    }

    def "toEntityメソッドでparamsがnullまたは空の場合に適切に処理されること"() {
        given: "パラメータが空のモデル"
        NotificationModel model = NotificationModel.reconstructFromEvent(
                10L, "0301", 100L, 200L, LocalDateTime.now(), 1
        )
        // 初期状態ではmessage、link等はnullなので設定
        model.setMessageAndLink(
                new com.hwhub.batch.domain.model.notification.NotificationMessage("title", "body", [:] as Map),
                new com.hwhub.batch.domain.model.notification.NotificationLink(NotificationLinkType.MY_TASKS, null)
        )

        when: "toEntityを呼び出す"
        TNotification entity = converter.toEntity(model)

        then: "JSON部分はnullになること"
        entity.getParamsJson() == null
        0 * objectMapper.writeValueAsString(_)
    }

    def "toEntityメソッドでJSONマッピングに失敗した場合、IllegalStateExceptionがスローされること"() {
        given: "JSON変換に失敗する状況"
        NotificationModel model = NotificationModel.reconstructFromEvent(
                10L, "0301", 100L, 200L, LocalDateTime.now(), 1
        )
        model.setMessageAndLink(
                new com.hwhub.batch.domain.model.notification.NotificationMessage("title", "body", ["k":"v"]),
                new com.hwhub.batch.domain.model.notification.NotificationLink(NotificationLinkType.MY_TASKS, null)
        )
        
        objectMapper.writeValueAsString(_) >> { throw new JsonProcessingException("error") {} }

        when: "toEntityを呼び出す"
        converter.toEntity(model)

        then: "IllegalStateExceptionが発生すること"
        def e = thrown(IllegalStateException)
        e.message == "Failed to serialize params_json"
    }

    def "toModel(TNotification)メソッドでエンティティがnullの場合、nullが返却されること"() {
        expect:
        converter.toModel((TNotification) null) == null
    }

    def "toModel(TNotification)メソッドでTNotificationからNotificationModelへマッピングされること"() {
        given: "TNotificationを準備"
        LocalDateTime occurredAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        LocalDateTime readAt = LocalDateTime.of(2023, 1, 2, 10, 0)
        
        TNotification entity = new TNotification()
        entity.setNotificationId(1L)
        entity.setHouseholdId(10L)
        entity.setNotificationType("0301")
        entity.setActorUserId(100L)
        entity.setTargetUserId(200L)
        entity.setIsRead(true)
        entity.setReadAt(java.util.Date.from(readAt.atZone(ZoneId.systemDefault()).toInstant()))
        entity.setTitleKey("title")
        entity.setBodyKey("body")
        entity.setParamsJson('{"key":"value"}')
        entity.setLinkType("MyTasks")
        entity.setLinkId(50L)
        entity.setOccurredAt(java.util.Date.from(occurredAt.atZone(ZoneId.systemDefault()).toInstant()))
        entity.setAggregatedKey("agg_key")
        entity.setAggregatedCount(5)

        Map<String, Object> paramsMap = ["key": "value"]
        objectMapper.readValue('{"key":"value"}', _ as TypeReference) >> paramsMap

        when: "toModelを呼び出す"
        NotificationModel model = converter.toModel(entity)

        then: "期待通りのフィールドが設定されること"
        model.getNotificationId().value() == 1L
        model.getHouseholdId() == 10L
        model.getNotificationType() == NotificationType.TASK_ASSIGNED
        model.getActorUserId() == 100L
        model.getTargetUserId() == 200L
        model.isRead()
        model.getReadAt() == readAt
        model.getMessage().titleKey() == "title"
        model.getMessage().bodyKey() == "body"
        model.getMessage().params() == paramsMap
        model.getLink().linkType() == NotificationLinkType.MY_TASKS
        model.getLink().linkId() == 50L
        model.getOccurredAt() == occurredAt
        model.getAggregatedKey() == "agg_key"
        model.getAggregatedCount() == 5
    }

    def "toModel(TNotification)メソッドでJSONがnullまたはエラーの場合に適切に処理されること"() {
        given: "JSONが存在しない、またはエラーが起こるエンティティ"
        TNotification entity1 = new TNotification()
        entity1.setNotificationId(1L)
        entity1.setHouseholdId(10L)
        entity1.setNotificationType("0301")
        entity1.setTargetUserId(200L)
        entity1.setTitleKey("title")
        entity1.setBodyKey("body")
        entity1.setOccurredAt(new java.util.Date())
        entity1.setLinkType("MyTasks")
        entity1.setAggregatedCount(1)
        entity1.setIsRead(false)
        entity1.setParamsJson(null)

        TNotification entity2 = new TNotification()
        entity2.setNotificationId(2L)
        entity2.setHouseholdId(10L)
        entity2.setNotificationType("0301")
        entity2.setTargetUserId(200L)
        entity2.setTitleKey("title")
        entity2.setBodyKey("body")
        entity2.setOccurredAt(new java.util.Date())
        entity2.setLinkType("MyTasks")
        entity2.setAggregatedCount(1)
        entity2.setIsRead(false)
        entity2.setParamsJson('invalid-json')
        
        objectMapper.readValue('invalid-json', _ as TypeReference) >> { throw new JsonProcessingException("err") {} }

        when: "toModelを呼び出す"
        NotificationModel model1 = converter.toModel(entity1)
        NotificationModel model2 = converter.toModel(entity2)

        then: "paramsはnullとして扱われること"
        model1.getMessage().params() == null
        model2.getMessage().params() == null
    }

    def "toModel(NotificationEventAggregationRow)メソッドで行がnullの場合、nullが返却されること"() {
        expect:
        converter.toModel((NotificationEventAggregationRow) null) == null
    }

    def "toModel(NotificationEventAggregationRow)メソッドでNotificationModelへマッピングされること"() {
        given: "Rowを準備"
        LocalDateTime occurredAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        NotificationEventAggregationRow row = new NotificationEventAggregationRow(
                10L, "0301", 100L, 200L, java.util.Date.from(occurredAt.atZone(ZoneId.systemDefault()).toInstant()), 2, null, null
        )

        when: "toModelを呼び出す"
        NotificationModel model = converter.toModel(row)

        then: "適切にマッピングされること"
        model.getHouseholdId() == 10L
        model.getNotificationType() == NotificationType.TASK_ASSIGNED
        model.getActorUserId() == 100L
        model.getTargetUserId() == 200L
        model.getOccurredAt() == occurredAt
        model.getAggregatedCount() == 2
        !model.isRead()
    }
}
