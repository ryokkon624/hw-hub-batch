package com.hwhub.batch.domain.model.notification

import com.hwhub.batch.domain.enums.NotificationLinkType
import spock.lang.Specification
import spock.lang.Unroll

class NotificationLinkSpec extends Specification {

    @Unroll
    def "正常系: NotificationLinkTypeが#linkTypeでlinkIdが#linkIdの場合、インスタンスが生成できること"() {
        when:
        def link = new NotificationLink(linkType, linkId)

        then:
        link.linkType() == linkType
        link.linkId() == linkId

        where:
        linkType                      | linkId
        NotificationLinkType.NONE     | null
        NotificationLinkType.MY_TASKS | 1L
        NotificationLinkType.MY_TASKS | null
    }

    def "正常系: noneメソッドを使用してNONEタイプのNotificationLinkが生成できること"() {
        when:
        def link = NotificationLink.none()

        then:
        link.linkType() == NotificationLinkType.NONE
        link.linkId() == null
    }

    def "異常系: linkTypeがnullの場合、NullPointerExceptionがスローされること"() {
        when:
        new NotificationLink(null, 1L)

        then:
        def e = thrown(NullPointerException)
        e.message == "linkType is required"
    }

    def "異常系: linkTypeがNONEかつlinkIdがnullでない場合、IllegalArgumentExceptionがスローされること"() {
        when:
        new NotificationLink(NotificationLinkType.NONE, 1L)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "linkId must be null when linkType is NONE"
    }
}
