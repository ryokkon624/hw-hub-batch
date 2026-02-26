package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.notification.NotificationModel;
import java.util.List;

public interface NotificationEventRepository {

  int markPendingAsProcessing(String proseccingKey, int limit, Long userId, String program);

  List<NotificationModel> aggregateLatestTaskAssignmentEvents(String proseccingKey);

  int markProcessingAsDone(String proseccingKey, Long userId, String program);
}
