package com.hwhub.batch.domain.model.inquiry;

import com.hwhub.batch.domain.enums.LocaleType;
import java.util.List;
import lombok.Getter;

@Getter
public class InquiryModel {
  private final InquiryId inquiryId;
  private final long userId;
  private final String title;
  private final String status;
  private final List<InquiryMessageModel> messages;
  private final LocaleType userLocale;

  private InquiryModel(
      InquiryId inquiryId,
      long userId,
      String title,
      String status,
      List<InquiryMessageModel> messages,
      LocaleType userLocale) {
    this.inquiryId = inquiryId;
    this.userId = userId;
    this.title = title;
    this.status = status;
    this.messages = messages;
    this.userLocale = userLocale;
  }

  public static InquiryModel reconstruct(
      long inquiryId,
      long userId,
      String title,
      String status,
      List<InquiryMessageModel> messages,
      LocaleType userLocale) {
    return new InquiryModel(new InquiryId(inquiryId), userId, title, status, messages, userLocale);
  }
}
