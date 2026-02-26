package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.HouseholdModel;
import java.util.List;

public interface HouseholdRepository {
  /**
   * メンバーが存在せず、updated_at が1ヶ月以上前の世帯を物理削除する。
   *
   * @param userId 実行ユーザID
   * @param program 実行プログラム
   * @return 削除件数
   */
  int deleteOrphanedHouseholds(Long userId, String program);

  List<HouseholdModel> findByIds(List<Long> householdIds);
}
