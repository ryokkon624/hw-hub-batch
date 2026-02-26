package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.HouseholdModel;
import com.hwhub.batch.domain.repository.HouseholdRepository;
import com.hwhub.batch.infrastructure.mybatis.converter.HouseholdConverter;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.HouseholdCustomMapper;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHousehold;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisHouseholdRepository implements HouseholdRepository {

  private final HouseholdCustomMapper customMapper;

  @Override
  public int deleteOrphanedHouseholds(Long userId, String program) {
    // 依存関係のある子テーブルから順に削除
    customMapper.deleteOrphanedAssignmentHistory();
    customMapper.deleteOrphanedRecalcRequests();
    customMapper.deleteOrphanedHouseworkTasks();
    customMapper.deleteOrphanedHouseworks();
    customMapper.deleteOrphanedShoppingItemAttachments();
    customMapper.deleteOrphanedShoppingItems();
    customMapper.deleteOrphanedInvitations();

    // 最後に親(m_household)を削除
    return customMapper.deleteOrphanedHouseholds();
  }

  @Override
  public List<HouseholdModel> findByIds(List<Long> householdIds) {
    List<MHousehold> entities = customMapper.findByIds(householdIds);
    return entities.stream().map(HouseholdConverter::toModel).toList();
  }
}
