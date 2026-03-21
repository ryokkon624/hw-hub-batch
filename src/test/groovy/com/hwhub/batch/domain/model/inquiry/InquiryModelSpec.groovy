package com.hwhub.batch.domain.model.inquiry

import com.hwhub.batch.domain.enums.LocaleType
import spock.lang.Specification

class InquiryModelSpec extends Specification {

    def "reconstructは全フィールドを正しくセットする"() {
        given:
        def msg = InquiryMessageModel.newMessage(new InquiryId(1L), 1, com.hwhub.batch.domain.enums.SenderType.YOU, "質問")

        when:
        def model = InquiryModel.reconstruct(5L, 2L, "件名", "00", [msg], LocaleType.JA)

        then:
        model.inquiryId.value() == 5L
        model.userId == 2L
        model.title == "件名"
        model.status == "00"
        model.messages.size() == 1
        model.userLocale == LocaleType.JA
    }

    def "reconstructはmessagesが空リストでも生成できる"() {
        when:
        def model = InquiryModel.reconstruct(1L, 1L, "件名", "00", [], LocaleType.EN)

        then:
        model != null
        model.messages.isEmpty()
        model.userLocale == LocaleType.EN
    }

    def "各LocaleTypeでreconstructできる"() {
        when:
        def model = InquiryModel.reconstruct(1L, 1L, "件名", "00", [], locale)

        then:
        model.userLocale == locale

        where:
        locale << LocaleType.values()
    }
}
