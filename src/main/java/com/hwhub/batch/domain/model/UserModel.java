package com.hwhub.batch.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class UserModel {
  private final Long userId;
  private final String email;
  private final String password;
  private final String passwordHash;
  private final LocalDateTime passwordChangedAt;
  private final String authProvider;
  private final String authProviderId;
  private final String displayName;
  private final String locale;
  private final String profileImageKey;
  private final String iconUrl;
  private final LocalDateTime emailVerifiedAt;
  private final boolean isActive;

  /**
   * 全プロパティを引数に取るコンストラクタ。
   *
   * @param userId ユーザID
   * @param email メールアドレス
   * @param password パスワード
   * @param passwordHash パスワードハッシュ
   * @param passwordChangedAt パスワード最終変更日時
   * @param authProvider 認証提供者
   * @param authProviderId 認証提供者ID
   * @param displayName 表示名
   * @param locale 利用言語
   * @param profileImageKey プロフィール画像ストレージキー
   * @param iconUrl アイコンのURL
   * @param emailVerifiedAt 認証完了日時
   * @param isActive 活性フラグ
   */
  private UserModel(
      Long userId,
      String email,
      String password,
      String passwordHash,
      LocalDateTime passwordChangedAt,
      String authProvider,
      String authProviderId,
      String displayName,
      String locale,
      String profileImageKey,
      String iconUrl,
      LocalDateTime emailVerifiedAt,
      boolean isActive) {
    this.userId = userId;
    this.email = email;
    this.password = password;
    this.passwordHash = passwordHash;
    this.passwordChangedAt = passwordChangedAt;
    this.authProvider = authProvider;
    this.authProviderId = authProviderId;
    this.displayName = displayName;
    this.locale = locale;
    this.profileImageKey = profileImageKey;
    this.iconUrl = iconUrl;
    this.emailVerifiedAt = emailVerifiedAt;
    this.isActive = isActive;
  }

  /**
   * 再構築・永続化用。infrastructure層からのみ呼び出されることを想定。
   *
   * @param userId ユーザID
   * @param email メールアドレス
   * @param passwordHash パスワードハッシュ
   * @param passwordChangedAt パスワード最終変更日時
   * @param authProvider 認証提供者
   * @param authProviderId 認証提供者ID
   * @param displayName 表示名
   * @param locale 利用言語
   * @param profileImageKey プロフィール画像ストレージキー
   * @param emailVerifiedAt 認証完了日時
   * @param isActive 活性フラグ
   * @return インスタンスを返す。
   */
  public static UserModel reconstruct(
      Long userId,
      String email,
      String passwordHash,
      LocalDateTime passwordChangedAt,
      String authProvider,
      String authProviderId,
      String displayName,
      String locale,
      String profileImageKey,
      LocalDateTime emailVerifiedAt,
      boolean isActive) {
    return new UserModel(
        userId,
        email,
        null,
        passwordHash,
        passwordChangedAt,
        authProvider,
        authProviderId,
        displayName,
        locale,
        profileImageKey,
        null,
        emailVerifiedAt,
        isActive);
  }
}
