package com.hwhub.batch.infrastructure.mybatis.converter;

import com.hwhub.batch.domain.model.HouseholdModel;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHousehold;

public final class HouseholdConverter {
  private HouseholdConverter() {}

  public static HouseholdModel toModel(MHousehold entity) {
    if (entity == null) {
      return null;
    }
    return HouseholdModel.reconstruct(
        entity.getHouseholdId(), entity.getName(), entity.getOwnerUserId());
  }

  public static MHousehold toEntity(HouseholdModel model) {
    MHousehold entity = new MHousehold();
    entity.setHouseholdId(model.getHouseholdId());
    entity.setName(model.getName());
    entity.setOwnerUserId(model.getOwnerUserId());

    return entity;
  }
}
