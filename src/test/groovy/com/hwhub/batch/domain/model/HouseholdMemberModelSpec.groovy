package com.hwhub.batch.domain.model

import spock.lang.Specification

class HouseholdMemberModelSpec extends Specification {

    def "reconstructメソッドで適切なフィールドを持つHouseholdMemberModelが生成できること"() {
        given: "パラメータを準備"
        Long householdId = 1L
        Long userId = 10L
        String displayName = "Original Name" // そのまま設定される
        String profileImageKey = "key"     // そのまま設定される
        String iconUrl = "url"             // そのまま設定される
        String nickname = "Family Nickname"
        String status = "ACTIVE"
        String role = "MEMBER"             // そのまま設定される

        when: "reconstructメソッドを呼び出す"
        HouseholdMemberModel memberModel = HouseholdMemberModel.reconstruct(
                householdId, userId, displayName, profileImageKey, iconUrl, nickname, status, role
        )

        then: "期待通りのフィールドが設定されていること"
        memberModel.getHouseholdId() == householdId
        memberModel.getUserId() == userId
        memberModel.getDisplayName() == displayName
        memberModel.getProfileImageKey() == profileImageKey
        memberModel.getIconUrl() == iconUrl
        memberModel.getNickname() == nickname
        memberModel.getStatus() == status
        memberModel.getRole() == role
    }
}
