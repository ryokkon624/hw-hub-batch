package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import org.apache.ibatis.annotations.Param;

public interface HouseholdInvitationCustomMapper {

  /**
   * 有効期限切れの PENDING 招待を EXPIRED に更新する
   *
   * @return 更新件数
   */
  int expirePendingInvitations(
      @Param("pendingStatus") String pendingStatus,
      @Param("expiredStatus") String expiredStatus,
      @Param("updateUserId") long updateUserId,
      @Param("updateProgram") String updateProgram);
}
