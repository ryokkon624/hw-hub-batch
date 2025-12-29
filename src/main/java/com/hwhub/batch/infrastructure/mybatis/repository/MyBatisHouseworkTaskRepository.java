package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.HouseworkTaskCreateParam;
import com.hwhub.batch.domain.repository.HouseworkTaskRepository;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.HouseworkTaskCustomMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisHouseworkTaskRepository implements HouseworkTaskRepository {

  private final HouseworkTaskCustomMapper customMapper;

  /**
   * 指定されたhouseworkIdのfrom以降に存在するtarget_dateを返す。
   *
   * @param houseworkId 家事ID
   * @param from From
   * @return 指定されたhouseworkIdのfrom以降に存在するtarget_date
   */
  @Override
  public List<LocalDate> findExistingTaskDates(Long houseworkId, LocalDate from) {
    return customMapper.selectExistingTaskDates(houseworkId, from);
  }

  /**
   * HouseworkTaskの一括登録。
   *
   * @param params HouseworkTask登録用パラメータ
   * @return 更新件数
   */
  @Override
  public int bulkInsertTasks(List<HouseworkTaskCreateParam> params) {
    if (params == null || params.isEmpty()) {
      return 0;
    }
    return customMapper.bulkInsertTasks(params);
  }

  /**
   * 指定された家事ID、日付のタスクのうち、ステータスが未対応のものを削除する。
   *
   * @param houseworkId 家事ID
   * @param dates 日付セット
   * @return 削除件数
   */
  @Override
  public int deleteUndoneTasksByDates(long houseworkId, Set<LocalDate> dates) {
    if (dates == null || dates.isEmpty()) {
      return 0;
    }
    return customMapper.deleteUndoneTasksByDates(houseworkId, dates);
  }

  /**
   * タスクに焼き付けられたスナップショットを更新する。
   *
   * @param houseworkId 家事ID
   * @param from 更新対象日付From
   * @param name 家事名
   * @param category カテゴリ
   * @param description 説明
   * @param userId 更新ユーザのユーザID
   * @param programName 更新プログラム名
   * @return 更新件数
   */
  @Override
  public int updateTaskSnapshot(
      long houseworkId,
      LocalDate from,
      String name,
      String description,
      String category,
      long userId,
      String programName) {
    return customMapper.updateTaskSnapshot(
        houseworkId, from, name, description, category, userId, programName);
  }

  /**
   * 指定された家事ID、from以降の担当者を更新する。 条件：ステータス=0:未対応 and 担当割り当て区分=9:システム割当
   *
   * @param houseworkId 家事ID
   * @param from 日付範囲From
   * @param newAssigneeUserId 更新する担当者のユーザID
   * @param userId 更新ユーザのユーザID
   * @param programName 更新プログラム名
   * @return 更新件数
   */
  @Override
  public int updateAssigneeForSystemAssigned(
      long houseworkId, LocalDate from, Long newAssigneeUserId, long userId, String programName) {
    return customMapper.updateAssigneeForSystemAssigned(
        houseworkId, from, newAssigneeUserId, userId, programName);
  }
}
