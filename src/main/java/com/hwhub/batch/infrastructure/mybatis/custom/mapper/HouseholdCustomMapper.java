package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import org.apache.ibatis.annotations.Mapper;

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
}
