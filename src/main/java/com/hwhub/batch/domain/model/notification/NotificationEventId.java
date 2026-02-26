package com.hwhub.batch.domain.model.notification;

public record NotificationEventId(long value) {
  public NotificationEventId {
    if (value <= 0) throw new IllegalArgumentException("notificationEventId must be positive");
  }
}
