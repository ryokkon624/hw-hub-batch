package com.hwhub.batch.domain.model.inquiry;

public record InquiryMessageId(long value) {
  public InquiryMessageId {
    if (value <= 0) throw new IllegalArgumentException("messageId must be positive");
  }
}
