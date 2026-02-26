package com.hwhub.batch.domain.model.notification

import spock.lang.Specification
import spock.lang.Unroll

class NotificationIdSpec extends Specification {

    @Unroll
    def "正常系: 有効な値(#value)でNotificationIdが生成できること"() {
        when:
        def notificationId = new NotificationId(value)

        then:
        notificationId.value() == value

        where:
        value || _
        1L    || _
        100L  || _
    }

    @Unroll
    def "異常系: 無効な値(#value)の場合、IllegalArgumentExceptionがスローされること"() {
        when:
        new NotificationId(value)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "notificationId must be positive"

        where:
        value || _
        0L    || _
        -1L   || _
    }
}
