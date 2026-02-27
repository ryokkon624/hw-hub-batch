package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.HouseholdMemberModel;
import java.util.List;

public interface HouseholdMemeberRepository {
  List<HouseholdMemberModel> findByHouseholdAndUserPairs(List<HouseholdUserPair> pairs);

  record HouseholdUserPair(Long householdId, Long userId) {}
}
