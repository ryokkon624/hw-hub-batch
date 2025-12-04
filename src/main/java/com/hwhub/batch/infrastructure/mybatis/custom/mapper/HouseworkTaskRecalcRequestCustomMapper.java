package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.domain.model.HouseworkTaskRecalcRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HouseworkTaskRecalcRequestCustomMapper {

  List<HouseworkTaskRecalcRequest> selectPendingRequests(@Param("limit") int limit);

  int updateStatusToDoneByRequestIds(
      @Param("requestIds") List<Long> requestIds,
      @Param("userId") Long userId,
      @Param("program") String program);

  int updateStatusToFailedByRequestIds(
      @Param("requestIds") List<Long> requestIds,
      @Param("message") String message,
      @Param("userId") Long userId,
      @Param("program") String program);
}
