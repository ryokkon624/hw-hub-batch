package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.enums.InvitationStatus;
import com.hwhub.batch.domain.repository.HouseholdInvitationRepository;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.HouseholdInvitationCustomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisHouseholdInvitationRepository implements HouseholdInvitationRepository {

  private final HouseholdInvitationCustomMapper customMapper;

  /**
   * 有効期限切れの PENDING 招待を EXPIRED に更新する
   *
   * @param userId 更新ユーザのユーザID
   * @param program 更新プログラム
   * @return 更新件数
   */
  @Override
  public int expirePendingInvitations(Long userId, String program) {

    return customMapper.expirePendingInvitations(
        InvitationStatus.PENDING.getCode(), InvitationStatus.EXPIRED.getCode(), userId, program);
  }
}
