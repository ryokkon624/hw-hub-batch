package com.hwhub.batch.infrastructure.mybatis.custom.entity;

import java.util.Date;

public record NotificationEventAggregationRow(
    Long householdId,
    String notificationType,
    Long actorUserId,
    Long targetUserId,
    Date latestOccurredAt,
    int taskCount,
    Date aggregationDate,
    Long latestHouseworkTaskId) {}
