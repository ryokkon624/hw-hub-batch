package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.domain.model.HouseworkTaskCreateParam;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

public interface HouseworkTaskCustomMapper {

  List<LocalDate> selectExistingTaskDates(
      @Param("houseworkId") Long houseworkId, @Param("fromDate") LocalDate fromDate);

  int bulkInsertTasks(@Param("list") List<HouseworkTaskCreateParam> list);

  int deleteUndoneTasksByDates(
      @Param("houseworkId") long houseworkId, @Param("dates") Set<LocalDate> dates);

  int updateTaskSnapshot(
      @Param("houseworkId") long houseworkId,
      @Param("fromDate") LocalDate fromDate,
      @Param("name") String name,
      @Param("description") String description,
      @Param("category") String category,
      @Param("userId") long userId,
      @Param("program") String program);

  int updateAssigneeForSystemAssigned(
      @Param("houseworkId") long houseworkId,
      @Param("fromDate") LocalDate fromDate,
      @Param("assigneeUserId") Long assigneeUserId,
      @Param("userId") long userId,
      @Param("program") String program);
}
