package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.notification.NotificationModel;
import com.hwhub.batch.domain.repository.NotificationEventRepository;
import com.hwhub.batch.infrastructure.mybatis.converter.NotificationConverter;
import com.hwhub.batch.infrastructure.mybatis.custom.entity.NotificationEventAggregationRow;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.NotificationEventCustomMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisNotificationEventRepository implements NotificationEventRepository {

  private final NotificationEventCustomMapper customMapper;
  private final NotificationConverter notificationConverter;

  @Override
  public int markPendingAsProcessing(String proseccingKey, int limit, Long userId, String program) {
    return customMapper.claimPendingEvents(proseccingKey, limit, userId, program);
  }

  @Override
  public List<NotificationModel> aggregateLatestTaskAssignmentEvents(String proseccingKey) {
    List<NotificationEventAggregationRow> rows =
        customMapper.aggregateLatestTaskAssignmentEvents(proseccingKey);
    return rows.stream().map(row -> notificationConverter.toModel(row)).toList();
  }

  @Override
  public int markProcessingAsDone(String proseccingKey, Long userId, String program) {
    return customMapper.markProcessingAsDone(proseccingKey, userId, program);
  }
}
