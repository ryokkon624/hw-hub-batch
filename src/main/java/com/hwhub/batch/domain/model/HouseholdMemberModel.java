package com.hwhub.batch.domain.model;

import lombok.Getter;

@Getter
public class HouseholdMemberModel {

  private final Long householdId;
  private final Long userId;
  private final String displayName;
  private final String profileImageKey;
  private final String iconUrl;
  private final String nickname;
  private final String status;
  private final String role;

  /**
   * 全プロパティを引数に取るコンストラクタ。
   *
   * @param householdId 世帯ID
   * @param userId ユーザID
   * @param displayName ユーザの表示名
   * @param profileImageKey ユーザのプロフィール画像キー
   * @param iconUrl プロフィール画像のUTL
   * @param nickname ニックネーム
   * @param status ステータス
   * @param role ロール
   */
  private HouseholdMemberModel(
      Long householdId,
      Long userId,
      String displayName,
      String profileImageKey,
      String iconUrl,
      String nickname,
      String status,
      String role) {
    this.householdId = householdId;
    this.userId = userId;
    this.displayName = displayName;
    this.profileImageKey = profileImageKey;
    this.iconUrl = iconUrl;
    this.nickname = nickname;
    this.status = status;
    this.role = role;
  }

  /**
   * 再構築・永続化用。infrastructure層からのみ呼び出されることを想定。
   *
   * @param householdId 世帯ID
   * @param userId ユーザID
   * @param displayName ユーザの表示名
   * @param profileImageKey ユーザのプロフィール画像キー
   * @param iconUrl プロフィール画像のUTL
   * @param nickname ニックネーム
   * @param status ステータス
   * @param role ロール
   * @return インスタンスを返す。
   */
  public static HouseholdMemberModel reconstruct(
      Long householdId,
      Long userId,
      String displayName,
      String profileImageKey,
      String iconUrl,
      String nickname,
      String status,
      String role) {
    return new HouseholdMemberModel(
        householdId, userId, displayName, profileImageKey, iconUrl, nickname, status, role);
  }
}
