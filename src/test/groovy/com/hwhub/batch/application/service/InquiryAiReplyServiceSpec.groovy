package com.hwhub.batch.application.service

import com.hwhub.batch.domain.ai.AiClient
import com.hwhub.batch.domain.enums.InquiryStatus
import com.hwhub.batch.domain.enums.LocaleType
import com.hwhub.batch.domain.enums.ProgramType
import com.hwhub.batch.domain.model.inquiry.InquiryModel
import com.hwhub.batch.domain.repository.InquiryRepository
import com.hwhub.batch.domain.repository.NotificationRepository
import com.hwhub.batch.infrastructure.s3.KnowledgeLoader
import spock.lang.Specification

class InquiryAiReplyServiceSpec extends Specification {

    InquiryRepository inquiryRepository = Mock()
    NotificationRepository notificationRepository = Mock()
    AiClient aiClient = Mock()
    KnowledgeLoader knowledgeLoader = Mock()

    InquiryAiReplyService service = new InquiryAiReplyService(
            inquiryRepository,
            notificationRepository,
            aiClient,
            knowledgeLoader
    )

    private InquiryModel makeInquiry(long id, long userId) {
        InquiryModel.reconstruct(id, userId, "件名${id}", InquiryStatus.OPEN.code, [], LocaleType.JA)
    }

    // ==================================
    // processAll - 対象0件
    // ==================================

    def "OPEN件数が0件の場合は正常終了し、successCount=0・failCount=0を返す"() {
        given:
        knowledgeLoader.load() >> "ナレッジ"

        when:
        def result = service.processAll()

        then:
        1 * inquiryRepository.findOpenList(_) >> []
        0 * aiClient.ask(_, _)
        0 * inquiryRepository.insertMessage(_, _, _)
        0 * inquiryRepository.updateStatus(_, _, _, _)
        0 * notificationRepository.insert(_, _, _)
        result.successCount() == 0
        result.failCount() == 0
    }

    // ==================================
    // processAll - 1件成功
    // ==================================

    def "OPEN 1件でAPIが成功した場合、insertMessage・updateStatus・通知発行が呼ばれ、successCount=1を返す"() {
        given:
        def inquiry = makeInquiry(10L, 5L)
        knowledgeLoader.load() >> "ナレッジ本文"

        when:
        def result = service.processAll()

        then:
        1 * inquiryRepository.findOpenList(_) >> [inquiry]
        1 * aiClient.ask(inquiry, "ナレッジ本文") >> "AI返答"
        1 * inquiryRepository.insertMessage({ it.body == "AI返答" && it.seq == 1 }, 1L, ProgramType.BTC_INQ_AI.code)
        1 * inquiryRepository.updateStatus(10L, InquiryStatus.AI_ANSWERED.code, 1L, ProgramType.BTC_INQ_AI.code)
        1 * notificationRepository.insert(
                { it.targetUserId == 5L && it.notificationType.name() == "YOUR_INQUIRY_HAS_BEEN_REPLIED" },
                1L,
                ProgramType.BTC_INQ_AI.code
        )
        result.successCount() == 1
        result.failCount() == 0
    }

    // ==================================
    // processAll - 複数件途中失敗
    // ==================================

    def "OPEN 3件で2件目にAPI失敗した場合、success=1・fail=2でbreakする"() {
        given:
        def inquiry1 = makeInquiry(1L, 1L)
        def inquiry2 = makeInquiry(2L, 2L)
        def inquiry3 = makeInquiry(3L, 3L)
        knowledgeLoader.load() >> "ナレッジ"

        when:
        def result = service.processAll()

        then:
        1 * inquiryRepository.findOpenList(_) >> [inquiry1, inquiry2, inquiry3]
        1 * aiClient.ask(inquiry1, _) >> "OK"
        1 * aiClient.ask(inquiry2, _) >> { throw new RuntimeException("API失敗") }
        0 * aiClient.ask(inquiry3, _)
        // 1件目はinsertMessage・updateStatus・通知が呼ばれる
        1 * inquiryRepository.insertMessage({ it.inquiryId.value() == 1L }, _, _)
        1 * inquiryRepository.updateStatus(1L, _, _, _)
        1 * notificationRepository.insert({ it.targetUserId == 1L }, _, _)
        // 2件目以降はinsertMessage呼ばれない
        0 * inquiryRepository.insertMessage({ it.inquiryId.value() == 2L }, _, _)
        result.successCount() == 1
        result.failCount() == 2
    }

    // ==================================
    // processAll - メッセージ履歴ありのseq計算
    // ==================================

    def "既存メッセージが2件ある問い合わせへの返信はseq=3になる"() {
        given:
        def msg1 = com.hwhub.batch.domain.model.inquiry.InquiryMessageModel.reconstruct(1L, 10L, 1, "USER", "質問1")
        def msg2 = com.hwhub.batch.domain.model.inquiry.InquiryMessageModel.reconstruct(2L, 10L, 2, "USER", "質問2")
        def inquiry = InquiryModel.reconstruct(10L, 5L, "件名", InquiryStatus.OPEN.code, [msg1, msg2], LocaleType.JA)
        knowledgeLoader.load() >> ""

        when:
        service.processAll()

        then:
        1 * inquiryRepository.findOpenList(_) >> [inquiry]
        1 * aiClient.ask(_, _) >> "AI回答"
        1 * inquiryRepository.insertMessage({ it.seq == 3 }, _, _)
        1 * inquiryRepository.updateStatus(_, _, _, _)
        1 * notificationRepository.insert(_, _, _)
    }

    // ==================================
    // processAll - ナレッジ空
    // ==================================

    def "ナレッジファイルが空の場合でもAPIを呼び出す"() {
        given:
        def inquiry = makeInquiry(5L, 2L)
        knowledgeLoader.load() >> ""

        when:
        service.processAll()

        then:
        1 * inquiryRepository.findOpenList(_) >> [inquiry]
        1 * aiClient.ask(inquiry, "") >> "回答"
        1 * inquiryRepository.insertMessage(_, _, _)
        1 * inquiryRepository.updateStatus(_, _, _, _)
        1 * notificationRepository.insert(_, _, _)
    }

    // ==================================
    // processAll - ProcessResult.batchSize
    // ==================================

    def "ProcessResultにはbatchSizeが含まれる"() {
        when:
        def result = service.processAll()

        then:
        1 * inquiryRepository.findOpenList(_) >> []
        result.batchSize() == 0  // @Value未設定のためデフォルト0
    }
}
