package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.notification.NotificationModel;
import com.hwhub.batch.domain.repository.NotificationRepository;
import com.hwhub.batch.infrastructure.mybatis.converter.NotificationConverter;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.NotificationCustomMapper;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.TNotification;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisNotificationRepository implements NotificationRepository {

  private final NotificationCustomMapper customMapper;
  private final NotificationConverter converter;

  @Override
  public int bulkInsert(List<NotificationModel> list, Long userId, String program) {
    List<TNotification> entities = list.stream().map(converter::toEntity).toList();
    entities.forEach(
        entity -> {
          entity.setCreateUserId(userId);
          entity.setCreateProgram(program);
          entity.setUpdateUserId(userId);
          entity.setUpdateProgram(program);
        });

    return customMapper.bulkInsert(entities);
  }
}
