package com.hwhub.batch.domain.model.inquiry

import spock.lang.Specification

class InquiryIdSpec extends Specification {

    // ==================================
    // InquiryId
    // ==================================

    def "InquiryIdは正の値で生成できる"() {
        expect:
        new InquiryId(value).value() == value

        where:
        value << [1L, 100L, Long.MAX_VALUE]
    }

    def "InquiryIdはvalue=0のときIllegalArgumentExceptionをスローする"() {
        when:
        new InquiryId(0L)

        then:
        thrown(IllegalArgumentException)
    }

    def "InquiryIdは負の値のときIllegalArgumentExceptionをスローする"() {
        when:
        new InquiryId(-1L)

        then:
        thrown(IllegalArgumentException)
    }

    // ==================================
    // InquiryMessageId
    // ==================================

    def "InquiryMessageIdは正の値で生成できる"() {
        expect:
        new InquiryMessageId(value).value() == value

        where:
        value << [1L, 50L, Long.MAX_VALUE]
    }

    def "InquiryMessageIdはvalue=0のときIllegalArgumentExceptionをスローする"() {
        when:
        new InquiryMessageId(0L)

        then:
        thrown(IllegalArgumentException)
    }

    def "InquiryMessageIdは負の値のときIllegalArgumentExceptionをスローする"() {
        when:
        new InquiryMessageId(-1L)

        then:
        thrown(IllegalArgumentException)
    }
}
