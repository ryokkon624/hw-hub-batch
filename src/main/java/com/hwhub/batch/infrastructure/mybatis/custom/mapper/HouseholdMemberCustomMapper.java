package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.domain.repository.HouseholdMemeberRepository.HouseholdUserPair;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHouseholdMember;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HouseholdMemberCustomMapper {

  List<MHouseholdMember> findByHouseholdAndUserPairs(@Param("pairs") List<HouseholdUserPair> pairs);
}
