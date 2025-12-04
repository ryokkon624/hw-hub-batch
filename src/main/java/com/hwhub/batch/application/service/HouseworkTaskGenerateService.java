package com.hwhub.batch.application.service;

import com.hwhub.batch.domain.enums.ProgramType;
import com.hwhub.batch.domain.enums.RecurrenceType;
import com.hwhub.batch.domain.model.Housework;
import com.hwhub.batch.domain.model.HouseworkTaskCreateParam;
import com.hwhub.batch.domain.repository.HouseworkRepository;
import com.hwhub.batch.domain.repository.HouseworkTaskRepository;
import com.hwhub.batch.domain.service.HouseworkScheduleCalculator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HouseworkTaskGenerateService {

  private static final Logger log = LoggerFactory.getLogger(HouseworkTaskGenerateService.class);

  private static final long SYSTEM_USER_ID = 2;

  private final HouseworkScheduleCalculator calculator;
  private final HouseworkRepository houseworkRepository;
  private final HouseworkTaskRepository houseworkTaskRepository;

  /**
   * 家事マスタから t_housework_task を生成するメイン処理
   *
   * @return 生成したタスク件数
   */
  @Transactional
  public int generateTasks() {

    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    // Weekly は today〜today+7日
    LocalDate weeklyTo = today.plusDays(7);
    // Monthly / NthWeekday は today〜today+1ヶ月+2日
    LocalDate monthlyTo = today.plusMonths(1).plusDays(2);
    log.info(
        "HouseworkTaskGenerateService started. today={}, weeklyTo={}, monthlyTo={}",
        today,
        weeklyTo,
        monthlyTo);

    // 有効期間が today〜monthlyTo と交差している housework を取得
    // Monthlyの期間でまとめて取得後、各処理で絞り込む
    List<Housework> houseworks = houseworkRepository.findActiveHouseworks(today, monthlyTo);
    if (houseworks.isEmpty()) {
      log.info("No active houseworks found in range. Nothing to do.");
      return 0;
    } else {
      log.info("Found {} active houseworks", houseworks.size());
    }

    int totalInserted = 0;
    for (Housework hw : houseworks) {

      // RecurrenceTypeによりtoを変更
      RecurrenceType type = RecurrenceType.fromCode(hw.getRecurrenceTypeCode());
      LocalDate rangeTo =
          switch (type) {
            case WEEKLY -> weeklyTo;
            case MONTHLY, NTH_WEEKDAY -> monthlyTo;
          };

      LocalDate from = max(today, hw.getStartDate());
      LocalDate to = min(rangeTo, hw.getEndDate());
      if (from.isAfter(to)) {
        continue;
      }

      // タスクの日付を生成
      List<LocalDate> candidateDates = calculator.calculateDates(hw, from, to);

      // 既存タスクのtarget_dateを取得し重複削除
      List<LocalDate> existingDates =
          houseworkTaskRepository.findExistingTaskDates(hw.getHouseworkId(), from);
      Set<LocalDate> existingSet = new HashSet<>(existingDates);

      List<LocalDate> newDates =
          candidateDates.stream().filter(d -> !existingSet.contains(d)).sorted().toList();

      if (!newDates.isEmpty()) {
        List<HouseworkTaskCreateParam> params =
            newDates.stream()
                .map(
                    d ->
                        HouseworkTaskCreateParam.from(
                            hw, d, SYSTEM_USER_ID, ProgramType.BTC_TSK_GEN.getCode()))
                .toList();
        int inserted = houseworkTaskRepository.bulkInsertTasks(params);
        totalInserted += inserted;

        log.debug(
            "Generated tasks for houseworkId={}, type={}, newDatesCount={}, inserted={}",
            hw.getHouseworkId(),
            type,
            newDates.size(),
            inserted);
      }
    }

    log.info("HouseworkTaskGenerateService finished. totalInserted={}", totalInserted);
    return totalInserted;
  }

  /**
   * 指定された日付のうち大きい日付を返す。
   *
   * @param a 日付
   * @param b 日付
   * @return 指定された日付のうち大きい日付
   */
  private static LocalDate max(LocalDate a, LocalDate b) {
    return a.isAfter(b) ? a : b;
  }

  /**
   * 指定された日付のうち小さい日付を返す。
   *
   * @param a 日付
   * @param b 日付
   * @return 指定された日付のうち小さい日付
   */
  private static LocalDate min(LocalDate a, LocalDate b) {
    return a.isBefore(b) ? a : b;
  }
}
