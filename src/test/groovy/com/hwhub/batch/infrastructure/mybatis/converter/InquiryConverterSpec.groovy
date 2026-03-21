package com.hwhub.batch.infrastructure.mybatis.converter

import com.hwhub.batch.domain.enums.LocaleType
import com.hwhub.batch.domain.enums.SenderType
import com.hwhub.batch.infrastructure.mybatis.custom.entity.InquiryWithMessagesEntity
import com.hwhub.batch.infrastructure.mybatis.generated.entity.TInquiryMessage
import spock.lang.Specification

class InquiryConverterSpec extends Specification {

    // ==================================
    // コンストラクタ
    // ==================================

    def "InquiryConverterはインスタンス化できる"() {
        when:
        def converter = new InquiryConverter()

        then:
        converter != null
    }

    // ==================================
    // toModel
    // ==================================

    def "toModelはInquiryWithMessagesEntityをInquiryModelに変換する（messagesあり）"() {
        given:
        def msgEntity = new TInquiryMessage()
        msgEntity.setMessageId(10L)
        msgEntity.setInquiryId(5L)
        msgEntity.setSeq(1)
        msgEntity.setSenderType("USER")
        msgEntity.setBody("ユーザーの質問")

        def entity = new InquiryWithMessagesEntity()
        entity.setInquiryId(5L)
        entity.setUserId(1L)
        entity.setTitle("件名")
        entity.setStatus("00")
        entity.setUserLocale("ja")
        entity.setMessages([msgEntity])

        when:
        def model = InquiryConverter.toModel(entity)

        then:
        model != null
        model.inquiryId.value() == 5L
        model.userId == 1L
        model.title == "件名"
        model.status == "00"
        model.userLocale == LocaleType.JA
        model.messages.size() == 1
        model.messages[0].senderType == SenderType.YOU
        model.messages[0].body == "ユーザーの質問"
    }

    def "toModelはmessagesがnullのとき空リストで変換する"() {
        given:
        def entity = new InquiryWithMessagesEntity()
        entity.setInquiryId(3L)
        entity.setUserId(2L)
        entity.setTitle("件名")
        entity.setStatus("00")
        entity.setUserLocale("ja")
        entity.setMessages(null)

        when:
        def model = InquiryConverter.toModel(entity)

        then:
        model != null
        model.messages.isEmpty()
    }

    def "toModelはuserLocaleがnullのときLocaleType.JAにフォールバックする"() {
        given:
        def entity = new InquiryWithMessagesEntity()
        entity.setInquiryId(1L)
        entity.setUserId(1L)
        entity.setTitle("件名")
        entity.setStatus("00")
        entity.setUserLocale(null)
        entity.setMessages([])

        when:
        def model = InquiryConverter.toModel(entity)

        then:
        model.userLocale == LocaleType.JA
    }

    def "toModelはuserLocaleが不正なコードのときLocaleType.JAにフォールバックする"() {
        given:
        def entity = new InquiryWithMessagesEntity()
        entity.setInquiryId(1L)
        entity.setUserId(1L)
        entity.setTitle("件名")
        entity.setStatus("00")
        entity.setUserLocale("xx")
        entity.setMessages([])

        when:
        def model = InquiryConverter.toModel(entity)

        then:
        model.userLocale == LocaleType.JA
    }

    def "toModelはuserLocaleがenのときLocaleType.ENに変換する"() {
        given:
        def entity = new InquiryWithMessagesEntity()
        entity.setInquiryId(2L)
        entity.setUserId(1L)
        entity.setTitle("件名")
        entity.setStatus("00")
        entity.setUserLocale("en")
        entity.setMessages([])

        when:
        def model = InquiryConverter.toModel(entity)

        then:
        model.userLocale == LocaleType.EN
    }

    // ==================================
    // toMessageModel
    // ==================================

    def "toMessageModelはTInquiryMessageをInquiryMessageModelに変換する"() {
        given:
        def entity = new TInquiryMessage()
        entity.setMessageId(10L)
        entity.setInquiryId(5L)
        entity.setSeq(2)
        entity.setSenderType("AI")
        entity.setBody("AI回答")

        when:
        def model = InquiryConverter.toMessageModel(entity)

        then:
        model != null
        model.messageId.value() == 10L
        model.inquiryId.value() == 5L
        model.seq == 2
        model.senderType == SenderType.AI_SUPPORT
        model.body == "AI回答"
    }

    def "toMessageModelは各SenderTypeを正しく変換する"() {
        given:
        def entity = new TInquiryMessage()
        entity.setMessageId(1L)
        entity.setInquiryId(1L)
        entity.setSeq(1)
        entity.setSenderType(senderTypeCode)
        entity.setBody("本文")

        when:
        def model = InquiryConverter.toMessageModel(entity)

        then:
        model.senderType == expectedSenderType

        where:
        senderTypeCode | expectedSenderType
        "USER"         | SenderType.YOU
        "AI"           | SenderType.AI_SUPPORT
        "STAFF"        | SenderType.STAFF
    }
}
