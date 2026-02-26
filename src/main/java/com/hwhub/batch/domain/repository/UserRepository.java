package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.UserModel;
import java.util.List;

public interface UserRepository {
  List<UserModel> findByIds(List<Long> userIds);
}
