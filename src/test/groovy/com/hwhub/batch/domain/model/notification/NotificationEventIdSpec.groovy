package com.hwhub.batch.domain.model.notification

import spock.lang.Specification
import spock.lang.Unroll

class NotificationEventIdSpec extends Specification {

    @Unroll
    def "正常系: 有効な値「#value」でNotificationEventIdが生成できること"() {
        when:
        def eventId = new NotificationEventId(value)

        then:
        eventId.value() == value

        where:
        value || _
        1L    || _
        100L  || _
    }

    @Unroll
    def "異常系: 無効な値「#value」の場合、IllegalArgumentExceptionがスローされること"() {
        when:
        new NotificationEventId(value)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "notificationEventId must be positive"

        where:
        value || _
        0L    || _
        -1L   || _
    }
}
