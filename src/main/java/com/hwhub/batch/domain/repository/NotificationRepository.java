package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.notification.NotificationModel;
import java.util.List;

public interface NotificationRepository {
  int bulkInsert(List<NotificationModel> list, Long userId, String program);

  /** 通知を1件登録する */
  void insert(NotificationModel model, Long userId, String program);
}
