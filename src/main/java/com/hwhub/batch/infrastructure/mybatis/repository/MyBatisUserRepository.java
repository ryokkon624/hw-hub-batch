package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.UserModel;
import com.hwhub.batch.domain.repository.UserRepository;
import com.hwhub.batch.infrastructure.mybatis.converter.UserConverter;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.UserCustomMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisUserRepository implements UserRepository {

  private final UserCustomMapper customMapper;

  @Override
  public List<UserModel> findByIds(List<Long> userIds) {
    return customMapper.findByIds(userIds).stream().map(UserConverter::toModel).toList();
  }
}
