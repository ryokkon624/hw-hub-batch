package com.hwhub.batch.domain.model.notification

import spock.lang.Specification
import spock.lang.Unroll

class NotificationMessageSpec extends Specification {

    @Unroll
    def "正常系: 有効な値でNotificationMessageが生成できること（params=#params）"() {
        when:
        def message = new NotificationMessage("title_key", "body_key", params)

        then:
        message.titleKey() == "title_key"
        message.bodyKey() == "body_key"
        message.params() == params

        where:
        params                        || _
        null                          || _
        Collections.emptyMap()        || _
        Map.of("key", "value")        || _
    }

    def "正常系: ofBodyメソッドでbodyKeyのみを持つインスタンスが生成できること"() {
        when:
        def message = NotificationMessage.ofBody("body_key")

        then:
        message.titleKey() == null
        message.bodyKey() == "body_key"
        message.params() == null
    }

    def "正常系: withParamsメソッドでparamsを追加した新しいインスタンスが生成できること"() {
        given:
        def originalMessage = new NotificationMessage("title_key", "body_key", null)
        def newParams = Map.of("key", "value")

        when:
        def newMessage = originalMessage.withParams(newParams)

        then:
        newMessage.titleKey() == "title_key"
        newMessage.bodyKey() == "body_key"
        newMessage.params() == newParams
        
        // 元のインスタンスが変更されていないこと
        originalMessage.params() == null
    }

    @Unroll
    def "異常系: bodyKeyが#descriptionの場合、IllegalArgumentExceptionがスローされること"() {
        when:
        new NotificationMessage("title_key", bodyKey, null)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "bodyKey is required"

        where:
        description | bodyKey
        "null"      | null
        "空文字"    | ""
        "空白文字"  | "   "
    }
}
