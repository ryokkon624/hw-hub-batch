package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.infrastructure.mybatis.custom.entity.NotificationEventAggregationRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationEventCustomMapper {

  /** PENDING を PROCESSING にして processingKey を付与して確保する */
  int claimPendingEvents(
      @Param("processingKey") String processingKey,
      @Param("limit") int limit,
      @Param("updateUserId") long updateUserId,
      @Param("updateProgram") String updateProgram);

  /**
   * processingKey で確保した集合から、task_id単位で最新を採用し、集約結果を返す
   *
   * <p>集約キー：target_user_id + notification_type_code + aggregation_date + actor_user_id
   */
  List<NotificationEventAggregationRow> aggregateLatestTaskAssignmentEvents(
      @Param("processingKey") String processingKey);

  /** processingKey の PROCESSING を DONE に更新する */
  int markProcessingAsDone(
      @Param("processingKey") String processingKey,
      @Param("updateUserId") long updateUserId,
      @Param("updateProgram") String updateProgram);
}
