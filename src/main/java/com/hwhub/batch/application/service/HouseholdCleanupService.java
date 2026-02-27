package com.hwhub.batch.application.service;

import com.hwhub.batch.domain.enums.ProgramType;
import com.hwhub.batch.domain.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseholdCleanupService {

  private static final long SYSTEM_USER_ID = 2;

  private final HouseholdRepository repository;

  @Transactional
  public int cleanupHouseholds() {
    log.info("Starting household cleanup...");

    // 1ヶ月以上更新がなく、メンバーが0人の世帯を物理削除する
    // 付随するデータもカスケード削除される（Repository実装内で処理）
    int deletedCount =
        repository.deleteOrphanedHouseholds(SYSTEM_USER_ID, ProgramType.BTC_HLD_CLEN.getCode());

    log.info("Finished household cleanup. Deleted count: {}", deletedCount);
    return deletedCount;
  }
}
