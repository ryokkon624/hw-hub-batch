package com.hwhub.batch.domain.model

import spock.lang.Specification

class HouseholdModelSpec extends Specification {

    def "reconstructメソッドで適切なフィールドを持つHouseholdModelが生成できること"() {
        given: "パラメータを準備"
        Long householdId = 1L
        String name = "Test Household"
        Long ownerUserId = 100L

        when: "reconstructメソッドを呼び出す"
        HouseholdModel householdModel = HouseholdModel.reconstruct(householdId, name, ownerUserId)

        then: "期待通りのフィールドが設定されていること"
        householdModel.getHouseholdId() == householdId
        householdModel.getName() == name
        householdModel.getOwnerUserId() == ownerUserId
    }
}
