package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.HouseworkTaskCreateParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface HouseworkTaskRepository {

    /**
     * 指定されたhouseworkIdのfrom以降に存在するtarget_dateを返す。
     *
     * @param houseworkId 家事ID
     * @param from From
     * @return 指定されたhouseworkIdのfrom以降に存在するtarget_date
     */
    List<LocalDate> findExistingTaskDates(Long houseworkId, LocalDate from);

    /**
     * HouseworkTaskの一括登録。
     *
     * @param params HouseworkTask登録用パラメータ
     * @return 更新件数
     */
    int bulkInsertTasks(List<HouseworkTaskCreateParam> params);

    /**
     * 指定された家事ID、日付のタスクのうち、ステータスが未対応のものを削除する。
     *
     * @param houseworkId 家事ID
     * @param dates 日付セット
     * @return 削除件数
     */
    int deleteUndoneTasksByDates(long houseworkId, Set<LocalDate> dates);

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
    int updateTaskSnapshot(long houseworkId,
                           LocalDate from,
                           String name,
                           String description,
                           String category,
                           long userId,
                           String programName);

    /**
     * 指定された家事ID、from以降の担当者を更新する。
     * 条件：ステータス=0:未対応 and 担当割り当て区分=9:システム割当
     *
     * @param houseworkId 家事ID
     * @param from 日付範囲From
     * @param newAssigneeUserId 更新する担当者のユーザID
     * @param userId 更新ユーザのユーザID
     * @param programName 更新プログラム名
     * @return 更新件数
     */
    int updateAssigneeForSystemAssigned(long houseworkId,
                                        LocalDate from,
                                        Long newAssigneeUserId,
                                        long userId,
                                        String programName);
}
