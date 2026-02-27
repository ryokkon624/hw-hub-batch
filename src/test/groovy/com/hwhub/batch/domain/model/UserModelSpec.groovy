package com.hwhub.batch.domain.model

import spock.lang.Specification

import java.time.LocalDateTime

class UserModelSpec extends Specification {

    def "reconstructメソッドで適切なフィールドを持つUserModelが生成できること"() {
        given: "必要なパラメータを準備"
        Long userId = 1L
        String email = "test@example.com"
        String passwordHash = "hashedPassword"
        LocalDateTime passwordChangedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        String authProvider = "local"
        String authProviderId = null
        String displayName = "Test User"
        String locale = "ja_JP"
        String profileImageKey = "profile/1.png"
        LocalDateTime emailVerifiedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        boolean isActive = true

        when: "reconstructメソッドを呼び出す"
        UserModel userModel = UserModel.reconstruct(
                userId, email, passwordHash, passwordChangedAt, authProvider, authProviderId,
                displayName, locale, profileImageKey, emailVerifiedAt, isActive
        )

        then: "期待通りのフィールドが設定されること"
        userModel.getUserId() == userId
        userModel.getEmail() == email
        userModel.getPassword() == null
        userModel.getPasswordHash() == passwordHash
        userModel.getPasswordChangedAt() == passwordChangedAt
        userModel.getAuthProvider() == authProvider
        userModel.getAuthProviderId() == authProviderId
        userModel.getDisplayName() == displayName
        userModel.getLocale() == locale
        userModel.getProfileImageKey() == profileImageKey
        userModel.getIconUrl() == null
        userModel.getEmailVerifiedAt() == emailVerifiedAt
        userModel.isActive() == isActive
    }
}
