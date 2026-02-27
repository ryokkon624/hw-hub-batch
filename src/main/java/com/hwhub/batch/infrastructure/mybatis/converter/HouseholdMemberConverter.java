package com.hwhub.batch.infrastructure.mybatis.converter;

import com.hwhub.batch.domain.model.HouseholdMemberModel;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHouseholdMember;

public final class HouseholdMemberConverter {
  private HouseholdMemberConverter() {}

  public static HouseholdMemberModel toModel(MHouseholdMember entity) {
    if (entity == null) return null;

    return HouseholdMemberModel.reconstruct(
        entity.getHouseholdId(),
        entity.getUserId(),
        null,
        null,
        null,
        entity.getNickname(),
        entity.getStatus(),
        null);
  }
}
