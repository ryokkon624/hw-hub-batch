package com.hwhub.batch.application.service;

import com.hwhub.batch.domain.enums.ProgramType;
import com.hwhub.batch.domain.enums.RecurrenceType;
import com.hwhub.batch.domain.model.Housework;
import com.hwhub.batch.domain.model.HouseworkTaskCreateParam;
import com.hwhub.batch.domain.model.HouseworkTaskRecalcRequest;
import com.hwhub.batch.domain.repository.HouseworkRepository;
import com.hwhub.batch.domain.repository.HouseworkTaskRecalcRequestRepository;
import com.hwhub.batch.domain.repository.HouseworkTaskRepository;
import com.hwhub.batch.domain.service.HouseworkScheduleCalculator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HouseworkTaskRecalcService {

  private static final Logger log = LoggerFactory.getLogger(HouseworkTaskRecalcService.class);

  private static final long SYSTEM_USER_ID = 2;

  private final HouseworkScheduleCalculator calculator;
  private final HouseworkTaskRecalcRequestRepository requestRepository;
  private final HouseworkTaskRepository taskRepository;
  private final HouseworkRepository houseworkRepository;

  /**
   * PENDING の houseworkId をまとめて再計算するエントリポイント。
   *
   * @param limit 1回の起動で処理する最大 housework 件数
   * @return 挿入/削除/更新されたタスク件数の合計
   */
  @Transactional
  public int recalcPending(int limit) {

    List<HouseworkTaskRecalcRequest> requests = requestRepository.findPendingRequests(limit);
    if (requests.isEmpty()) {
      log.info("No pending housework task recalc requests.");
      return 0;
    }

    log.info("Start recalc for {} houseworks: {}", requests.size(), requests);

    // houseworkId毎に集約
    Map<Long, List<Long>> houseworkId2RequestIds =
        requests.stream()
            .collect(
                Collectors.groupingBy(
                    HouseworkTaskRecalcRequest::houseworkId,
                    Collectors.mapping(
                        HouseworkTaskRecalcRequest::requestId, Collectors.toList())));

    int totalAffected = 0;
    for (Map.Entry<Long, List<Long>> entry : houseworkId2RequestIds.entrySet()) {
      long houseworkId = entry.getKey();
      List<Long> requestIds = entry.getValue();

      try {
        int affected = recalc4Housework(houseworkId);
        totalAffected += affected;
        requestRepository.markDoneByRequestIds(
            requestIds, SYSTEM_USER_ID, ProgramType.BTC_TSK_RECL.getCode());
      } catch (Exception e) {
        log.error("Failed to recalc housework tasks for houseworkId={}", houseworkId, e);
        requestRepository.markFailedByRequestIds(
            requestIds,
            Optional.ofNullable(e.getMessage()).orElse("unknown error"),
            SYSTEM_USER_ID,
            ProgramType.BTC_TSK_RECL.getCode());
      }
    }

    log.info("Finished recalc. totalAffected={}", totalAffected);
    return totalAffected;
  }

  /**
   * 家事マスタの定義の基づき、生成済みの家事タスクを再構成する。
   *
   * @param houseworkId 家事ID
   * @return 反映件数
   */
  private int recalc4Housework(long houseworkId) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));

    Housework hw =
        houseworkRepository
            .findById(houseworkId)
            .orElseThrow(() -> new IllegalStateException("Housework not found: " + houseworkId));

    RecurrenceType type = RecurrenceType.fromCode(hw.getRecurrenceTypeCode());
    // タスクを生成する範囲
    LocalDate rangeTo =
        switch (type) {
          case WEEKLY -> today.plusDays(7);
          case MONTHLY, NTH_WEEKDAY -> today.plusMonths(1).plusDays(2);
        };

    LocalDate from = max(today, hw.getStartDate());
    LocalDate to = min(rangeTo, hw.getEndDate());
    if (from.isAfter(to)) {
      log.info("No target range for houseworkId={} (from={}, to={})", houseworkId, from, to);
      return 0;
    }

    List<LocalDate> expectedDates = calculator.calculateDates(hw, from, to);

    if (expectedDates.isEmpty()) {
      log.info("No expected dates for houseworkId={} in range {}..{}", houseworkId, from, to);
      return 0;
    }

    // 生成済みのタスクの日付を取得
    List<LocalDate> existingDates = taskRepository.findExistingTaskDates(houseworkId, today);

    Set<LocalDate> expectedSet = new HashSet<>(expectedDates);
    Set<LocalDate> existingSet = new HashSet<>(existingDates);

    // 新規作成する日付
    Set<LocalDate> toCreateDates = new HashSet<>(expectedSet);
    toCreateDates.removeAll(existingSet);

    // 削除する日付
    Set<LocalDate> toDeleteDates = new HashSet<>(existingSet);
    toDeleteDates.removeAll(expectedSet);

    // スナップショットを更新
    int snapshotUpdated =
        taskRepository.updateTaskSnapshot(
            houseworkId,
            from,
            hw.getName(),
            hw.getDescription(),
            hw.getCategory(),
            SYSTEM_USER_ID,
            ProgramType.BTC_TSK_RECL.getCode());

    // default担当者が変更されている場合、未対応で担当者の割当をしていないレコードを更新
    int assigneeUpdated = 0;
    if (hw.getDefaultAssigneeUserId() != null) {
      assigneeUpdated =
          taskRepository.updateAssigneeForSystemAssigned(
              houseworkId,
              from,
              hw.getDefaultAssigneeUserId(),
              SYSTEM_USER_ID,
              ProgramType.BTC_TSK_RECL.getCode());
    }

    // 削除
    int deleted = taskRepository.deleteUndoneTasksByDates(houseworkId, toDeleteDates);

    // 追加
    List<HouseworkTaskCreateParam> createParams =
        toCreateDates.stream()
            .map(
                date ->
                    HouseworkTaskCreateParam.from(
                        hw, date, SYSTEM_USER_ID, ProgramType.BTC_TSK_RECL.getCode()))
            .toList();
    int inserted = taskRepository.bulkInsertTasks(createParams);

    log.info(
        "Recalc houseworkId={} from={} to={}: snapshotUpdated={}, assigneeUpdated={}, deleted={}, inserted={}",
        houseworkId,
        from,
        to,
        snapshotUpdated,
        assigneeUpdated,
        deleted,
        inserted);

    return snapshotUpdated + assigneeUpdated + deleted + inserted;
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
