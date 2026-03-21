package com.hwhub.batch.infrastructure.ai

import com.hwhub.batch.domain.enums.LocaleType
import com.hwhub.batch.domain.enums.SenderType
import com.hwhub.batch.domain.model.inquiry.InquiryId
import com.hwhub.batch.domain.model.inquiry.InquiryMessageModel
import com.hwhub.batch.domain.model.inquiry.InquiryModel
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.RestClient
import spock.lang.Specification

class ClaudeApiClientSpec extends Specification {

    // RequestBodyUriSpec extends RequestBodySpec extends RequestHeadersSpec,
    // なので単一のモックでチェーン全体をカバーできる
    RestClient restClient = Mock()
    RestClient.RequestBodyUriSpec requestSpec = Mock()
    RestClient.ResponseSpec responseSpec = Mock()

    ClaudeApiClient client

    def setup() {
        client = new ClaudeApiClient(restClient)
        ReflectionTestUtils.setField(client, "apiKey", "test-key")
        ReflectionTestUtils.setField(client, "model", "claude-3")
        ReflectionTestUtils.setField(client, "maxTokens", 1024)
        ReflectionTestUtils.setField(client, "systemPrompt", "あなたはサポートです。")

        // RestClientチェーン共通スタブ (body()以外)
        restClient.post() >> requestSpec
        requestSpec.uri(_) >> requestSpec
        requestSpec.header(_, _) >> requestSpec
        requestSpec.contentType(_) >> requestSpec
        requestSpec.retrieve() >> responseSpec
    }

    private InquiryModel makeInquiry(long id, long userId, LocaleType locale) {
        InquiryModel.reconstruct(id, userId, "件名", "00", [], locale)
    }

    // ==================================
    // ask - API成功
    // ==================================

    def "API呼び出しが成功した場合、レスポンスのtextを返す"() {
        given:
        def inquiry = makeInquiry(5L, 1L, LocaleType.JA)
        def responseBody = [content: [[text: "AI返答テキスト"]]]
        requestSpec.body(_) >> requestSpec
        responseSpec.body(_ as ParameterizedTypeReference) >> responseBody

        when:
        def result = client.ask(inquiry, "ナレッジ")

        then:
        result == "AI返答テキスト"
    }

    // ==================================
    // ask - API失敗
    // ==================================

    def "API呼び出しで例外が発生した場合、RuntimeExceptionでラップして再スローする"() {
        given:
        def inquiry = makeInquiry(7L, 2L, LocaleType.JA)
        requestSpec.body(_) >> requestSpec
        responseSpec.body(_ as ParameterizedTypeReference) >> { throw new RuntimeException("network error") }

        when:
        client.ask(inquiry, "ナレッジ")

        then:
        def ex = thrown(RuntimeException)
        ex.message.contains("Claude API 呼び出し失敗")
        ex.message.contains("7")
    }

    // ==================================
    // ask - locale別言語指示
    // ==================================

    def "ロケールJAの場合、systemに日本語指示が含まれる"() {
        given:
        def inquiry = makeInquiry(1L, 1L, LocaleType.JA)
        responseSpec.body(_ as ParameterizedTypeReference) >> [content: [[text: "回答"]]]

        when:
        client.ask(inquiry, "ナレッジ")

        then:
        1 * requestSpec.body({ Map body ->
            (body["system"] as String).contains("必ず日本語で回答してください。")
        }) >> requestSpec
    }

    def "ロケールENの場合、systemに英語指示が含まれる"() {
        given:
        def inquiry = makeInquiry(2L, 1L, LocaleType.EN)
        responseSpec.body(_ as ParameterizedTypeReference) >> [content: [[text: "answer"]]]

        when:
        client.ask(inquiry, "ナレッジ")

        then:
        1 * requestSpec.body({ Map body ->
            (body["system"] as String).contains("Please respond in English.")
        }) >> requestSpec
    }

    def "ロケールESの場合、systemにスペイン語指示が含まれる"() {
        given:
        def inquiry = makeInquiry(3L, 1L, LocaleType.ES)
        responseSpec.body(_ as ParameterizedTypeReference) >> [content: [[text: "respuesta"]]]

        when:
        client.ask(inquiry, "ナレッジ")

        then:
        1 * requestSpec.body({ Map body ->
            (body["system"] as String).contains("Por favor responda en español.")
        }) >> requestSpec
    }

    // ==================================
    // ask - リクエストボディの内容
    // ==================================

    def "リクエストボディにmodel・max_tokens・systemが含まれる"() {
        given:
        def inquiry = makeInquiry(1L, 1L, LocaleType.JA)
        responseSpec.body(_ as ParameterizedTypeReference) >> [content: [[text: "回答"]]]

        when:
        client.ask(inquiry, "FAQナレッジ")

        then:
        1 * requestSpec.body({ Map body ->
            body["model"] == "claude-3" &&
            body["max_tokens"] == 1024 &&
            (body["system"] as String).contains("あなたはサポートです。") &&
            (body["system"] as String).contains("FAQナレッジ")
        }) >> requestSpec
    }

    def "メッセージ履歴がある問い合わせはuserContentに履歴が含まれる"() {
        given:
        def msg = InquiryMessageModel.reconstruct(1L, 5L, 1, "USER", "ユーザーの質問")
        def inquiry = InquiryModel.reconstruct(5L, 1L, "件名テスト", "00", [msg], LocaleType.JA)
        responseSpec.body(_ as ParameterizedTypeReference) >> [content: [[text: "回答"]]]

        when:
        client.ask(inquiry, "ナレッジ")

        then:
        1 * requestSpec.body({ Map body ->
            def messages = body["messages"] as List
            def userContent = messages[0]["content"] as String
            userContent.contains("件名テスト") && userContent.contains("ユーザーの質問")
        }) >> requestSpec
    }

    // ==================================
    // ask - ヘッダーの確認
    // ==================================

    def "x-api-keyヘッダーにapiKeyが設定される"() {
        given:
        def inquiry = makeInquiry(1L, 1L, LocaleType.JA)
        requestSpec.body(_) >> requestSpec
        responseSpec.body(_ as ParameterizedTypeReference) >> [content: [[text: "回答"]]]

        when:
        client.ask(inquiry, "ナレッジ")

        then:
        1 * requestSpec.header("x-api-key", "test-key") >> requestSpec
    }
}
