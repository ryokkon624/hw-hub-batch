package com.hwhub.batch.infrastructure.mybatis.custom.entity;

import com.hwhub.batch.infrastructure.mybatis.generated.entity.TInquiryMessage;
import java.util.List;
import lombok.Data;

/** t_inquiry と t_inquiry_message を LEFT JOIN した結果を受け取る専用Entity */
@Data
public class InquiryWithMessagesEntity {
  private Long inquiryId;
  private Long userId;
  private String title;
  private String status;
  private String userLocale;
  private List<TInquiryMessage> messages;
}
