package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.Housework;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HouseworkRepository {

    /**
     * 有効期間がfrom、toと交差している家事マスタを取得する。
     *
     * @param from From
     * @param to To
     * @return 有効期間がfrom、toと交差している家事マスタ
     */
    List<Housework> findActiveHouseworks(LocalDate from, LocalDate to);

    /**
     * 指定された家事IDの家事マスタを取得する。
     *
     * @param houseworkId 家事ID
     * @return 指定された家事IDの家事マスタ
     */
    Optional<Housework> findById(Long houseworkId);
}
