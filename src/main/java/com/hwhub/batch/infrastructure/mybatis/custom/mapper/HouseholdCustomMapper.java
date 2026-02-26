package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHousehold;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HouseholdCustomMapper {
  // Delete main
  int deleteOrphanedHouseholds();

  // Delete related
  int deleteOrphanedAssignmentHistory();

  int deleteOrphanedHouseworkTasks();

  int deleteOrphanedRecalcRequests();

  int deleteOrphanedHouseworks();

  int deleteOrphanedShoppingItemAttachments();

  int deleteOrphanedShoppingItems();

  int deleteOrphanedInvitations();

  List<MHousehold> findByIds(@Param("ids") List<Long> ids);
}
