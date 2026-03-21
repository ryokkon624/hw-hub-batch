package com.hwhub.batch.application.service;

import com.hwhub.batch.domain.ai.AiClient;
import com.hwhub.batch.domain.enums.InquiryStatus;
import com.hwhub.batch.domain.enums.NotificationLinkType;
import com.hwhub.batch.domain.enums.NotificationType;
import com.hwhub.batch.domain.enums.ProgramType;
import com.hwhub.batch.domain.enums.SenderType;
import com.hwhub.batch.domain.model.inquiry.InquiryMessageModel;
import com.hwhub.batch.domain.model.inquiry.InquiryModel;
import com.hwhub.batch.domain.model.notification.NotificationLink;
import com.hwhub.batch.domain.model.notification.NotificationMessage;
import com.hwhub.batch.domain.model.notification.NotificationModel;
import com.hwhub.batch.domain.repository.InquiryRepository;
import com.hwhub.batch.domain.repository.NotificationRepository;
import com.hwhub.batch.infrastructure.s3.KnowledgeLoader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryAiReplyService {

  private final InquiryRepository inquiryRepository;
  private final NotificationRepository notificationRepository;
  private final AiClient aiClient;
  private final KnowledgeLoader knowledgeLoader;

  @Value("${hwhub.ai.inquiry-reply.batch-size:100}")
  private int batchSize;

  private static final long SYSTEM_USER_ID = 1L;

  /**
   * OPEN な問い合わせに AI 返信する。 1件でも API 失敗したら後続は停止。 それまでの成功件数・失敗件数をログ出力。
   *
   * @return 処理結果サマリー
   */
  @Transactional
  public ProcessResult processAll() {
    List<InquiryModel> targets = inquiryRepository.findOpenList(batchSize);
    if (targets.isEmpty()) {
      log.info("No open inquiries to process.");
      return new ProcessResult(0, 0, batchSize);
    }

    log.info("Starting AI reply process. target={}", targets.size());

    // ナレッジは全件共通で1回だけ読み込む
    String knowledge = knowledgeLoader.load();

    int successCount = 0;
    int failCount = 0;

    for (InquiryModel inquiry : targets) {
      try {
        String answer = aiClient.ask(inquiry, knowledge);

        int nextSeq = inquiry.getMessages().size() + 1;
        InquiryMessageModel message =
            InquiryMessageModel.newMessage(
                inquiry.getInquiryId(), nextSeq, SenderType.AI_SUPPORT, answer);
        inquiryRepository.insertMessage(message, SYSTEM_USER_ID, ProgramType.BTC_INQ_AI.getCode());

        inquiryRepository.updateStatus(
            inquiry.getInquiryId().value(),
            InquiryStatus.AI_ANSWERED.getCode(),
            SYSTEM_USER_ID,
            ProgramType.BTC_INQ_AI.getCode());

        publishReplyNotification(inquiry);

        successCount++;
        log.info(
            "AI reply succeeded. inquiryId={} seq={}", inquiry.getInquiryId().value(), nextSeq);

      } catch (Exception e) {
        failCount = targets.size() - successCount;
        log.error(
            "AI reply failed. Stopping subsequent processing. inquiryId={} success={} fail={} (including remaining)",
            inquiry.getInquiryId().value(),
            successCount,
            failCount,
            e);
        break;
      }
    }

    ProcessResult result = new ProcessResult(successCount, failCount, batchSize);
    log.info(
        "InquiryAiReplyJob finished. success={} fail={} batchSize={}",
        result.successCount(),
        result.failCount(),
        result.batchSize());
    return result;
  }

  public record ProcessResult(int successCount, int failCount, int batchSize) {}

  private void publishReplyNotification(InquiryModel inquiry) {
    Map<String, Object> params = new HashMap<>();
    params.put("inquiryId", inquiry.getInquiryId().value());
    params.put("title", inquiry.getTitle());

    NotificationMessage message =
        new NotificationMessage(
            "notifications.messages.inquiryReplied.title",
            "notifications.messages.inquiryReplied.body",
            params);

    NotificationLink link =
        new NotificationLink(NotificationLinkType.INQUIRY_DETAIL, inquiry.getInquiryId().value());

    NotificationModel model =
        NotificationModel.newUnread(
            null,
            NotificationType.YOUR_INQUIRY_HAS_BEEN_REPLIED,
            null,
            inquiry.getUserId(),
            message,
            link,
            LocalDateTime.now());

    notificationRepository.insert(model, SYSTEM_USER_ID, ProgramType.BTC_INQ_AI.getCode());
  }
}
