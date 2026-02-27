package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.HouseholdMemberModel;
import com.hwhub.batch.domain.repository.HouseholdMemeberRepository;
import com.hwhub.batch.infrastructure.mybatis.converter.HouseholdMemberConverter;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.HouseholdMemberCustomMapper;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHouseholdMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisHouseholdMemeberRepository implements HouseholdMemeberRepository {

  private final HouseholdMemberCustomMapper customMapper;

  @Override
  public List<HouseholdMemberModel> findByHouseholdAndUserPairs(List<HouseholdUserPair> pairs) {
    List<MHouseholdMember> entities = customMapper.findByHouseholdAndUserPairs(pairs);
    return entities.stream().map(HouseholdMemberConverter::toModel).toList();
  }
}
