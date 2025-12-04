package com.hwhub.batch.domain.repository;

public interface HouseholdInvitationRepository {

  /**
   * 有効期限切れの PENDING 招待を EXPIRED に更新する
   *
   * @param userId 更新ユーザのユーザID
   * @param program 更新プログラム
   * @return 更新件数
   */
  int expirePendingInvitations(Long userId, String program);
}
