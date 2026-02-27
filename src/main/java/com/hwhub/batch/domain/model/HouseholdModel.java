package com.hwhub.batch.domain.model;

import lombok.Getter;

@Getter
public class HouseholdModel {
  private final Long householdId;
  private final String name;
  private final Long ownerUserId;

  /**
   * 全プロパティを引数に取るコンストラクタ。
   *
   * @param householdId 世帯ID
   * @param name 世帯名
   * @param ownerUserId 世帯所有者
   */
  private HouseholdModel(Long householdId, String name, Long ownerUserId) {
    this.householdId = householdId;
    this.name = name;
    this.ownerUserId = ownerUserId;
  }

  /**
   * 再構築・永続化用。infrastructure層からのみ呼び出されることを想定。
   *
   * @param householdId 世帯ID
   * @param name 世帯名
   * @param ownerUserId 世帯所有者
   * @return インスタンスを返す。
   */
  public static HouseholdModel reconstruct(Long householdId, String name, Long ownerUserId) {
    return new HouseholdModel(householdId, name, ownerUserId);
  }
}
