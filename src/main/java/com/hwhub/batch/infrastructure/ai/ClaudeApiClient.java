package com.hwhub.batch.infrastructure.ai;

import com.hwhub.batch.domain.ai.AiClient;
import com.hwhub.batch.domain.enums.LocaleType;
import com.hwhub.batch.domain.model.inquiry.InquiryMessageModel;
import com.hwhub.batch.domain.model.inquiry.InquiryModel;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeApiClient implements AiClient {

  @Value("${hwhub.ai.claude.api-key}")
  private String apiKey;

  @Value("${hwhub.ai.claude.model}")
  private String model;

  @Value("${hwhub.ai.claude.max-tokens}")
  private int maxTokens;

  @Value("${hwhub.ai.claude.system-prompt}")
  private String systemPrompt;

  private static final Map<LocaleType, String> LOCALE_INSTRUCTION =
      Map.of(
          LocaleType.JA, "必ず日本語で回答してください。",
          LocaleType.EN, "Please respond in English.",
          LocaleType.ES, "Por favor responda en español.");

  private final RestClient restClient;

  @Override
  public String ask(InquiryModel inquiry, String knowledge) {
    try {
      String localeInstruction =
          LOCALE_INSTRUCTION.getOrDefault(
              inquiry.getUserLocale(), LOCALE_INSTRUCTION.get(LocaleType.JA));

      Map<String, Object> body =
          Map.of(
              "model",
              model,
              "max_tokens",
              maxTokens,
              "system",
              systemPrompt + "\n\n## ナレッジベース\n" + knowledge + "\n\n## 言語指示\n" + localeInstruction,
              "messages",
              List.of(Map.of("role", "user", "content", buildUserMessage(inquiry))));

      Map<String, Object> response =
          restClient
              .post()
              .uri("https://api.anthropic.com/v1/messages")
              .header("x-api-key", apiKey)
              .header("anthropic-version", "2023-06-01")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
      return (String) content.get(0).get("text");

    } catch (Exception e) {
      throw new RuntimeException("Claude API 呼び出し失敗 inquiryId=" + inquiry.getInquiryId(), e);
    }
  }

  private String buildUserMessage(InquiryModel inquiry) {
    StringBuilder sb = new StringBuilder();
    sb.append("件名: ").append(inquiry.getTitle()).append("\n\n");
    sb.append("## メッセージ履歴\n");
    for (InquiryMessageModel msg : inquiry.getMessages()) {
      sb.append("[").append(msg.getSenderType()).append("] ");
      sb.append(msg.getBody()).append("\n");
    }
    return sb.toString();
  }
}
