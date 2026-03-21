package com.hwhub.batch.domain.model.inquiry

import com.hwhub.batch.domain.enums.SenderType
import spock.lang.Specification

class InquiryMessageModelSpec extends Specification {

    // ==================================
    // newMessage
    // ==================================

    def "newMessageはmessageId=nullで生成する"() {
        given:
        def inquiryId = new InquiryId(5L)

        when:
        def model = InquiryMessageModel.newMessage(inquiryId, 1, SenderType.AI_SUPPORT, "AI返答")

        then:
        model.messageId == null
        model.inquiryId.value() == 5L
        model.seq == 1
        model.senderType == SenderType.AI_SUPPORT
        model.body == "AI返答"
    }

    def "newMessageは各SenderTypeで生成できる"() {
        when:
        def model = InquiryMessageModel.newMessage(new InquiryId(1L), 1, senderType, "本文")

        then:
        model.senderType == senderType

        where:
        senderType << SenderType.values()
    }

    // ==================================
    // reconstruct
    // ==================================

    def "reconstructはmessageIdとinquiryIdを正しくセットする"() {
        when:
        def model = InquiryMessageModel.reconstruct(10L, 5L, 3, "STAFF", "スタッフ返信")

        then:
        model.messageId.value() == 10L
        model.inquiryId.value() == 5L
        model.seq == 3
        model.senderType == SenderType.STAFF
        model.body == "スタッフ返信"
    }

    def "reconstructは各senderTypeコードを正しく変換する"() {
        when:
        def model = InquiryMessageModel.reconstruct(1L, 1L, 1, senderTypeCode, "本文")

        then:
        model.senderType == expectedSenderType

        where:
        senderTypeCode | expectedSenderType
        "USER"         | SenderType.YOU
        "AI"           | SenderType.AI_SUPPORT
        "STAFF"        | SenderType.STAFF
    }
}
