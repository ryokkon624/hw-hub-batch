package com.hwhub.batch.infrastructure.mybatis.converter;

import com.hwhub.batch.domain.model.UserModel;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MUser;

public final class UserConverter {

  private UserConverter() {}

  public static UserModel toModel(MUser entity) {
    if (entity == null) {
      return null;
    }

    return UserModel.reconstruct(
        entity.getUserId(),
        entity.getEmail(),
        entity.getPasswordHash(),
        DateConverter.toLocalDateTime(entity.getPasswordChangedAt()),
        entity.getAuthProvider(),
        entity.getAuthProviderId(),
        entity.getDisplayName(),
        entity.getLocale(),
        entity.getProfileImageKey(),
        DateConverter.toLocalDateTime(entity.getEmailVerifiedAt()),
        entity.getIsActive());
  }
}
