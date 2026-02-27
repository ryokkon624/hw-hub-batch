package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.infrastructure.mybatis.generated.entity.TNotification;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationCustomMapper {
  int bulkInsert(@Param("list") List<TNotification> list);
}
