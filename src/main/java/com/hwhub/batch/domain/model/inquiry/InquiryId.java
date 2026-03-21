package com.hwhub.batch.domain.model.inquiry;

public record InquiryId(long value) {
  public InquiryId {
    if (value <= 0) throw new IllegalArgumentException("inquiryId must be positive");
  }
}
