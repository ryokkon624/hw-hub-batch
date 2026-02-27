package com.hwhub.batch.infrastructure.mybatis.converter

import com.hwhub.batch.domain.model.HouseholdMemberModel
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHouseholdMember
import spock.lang.Specification

class HouseholdMemberConverterSpec extends Specification {

    def "toModelメソッドでエンティティがnullの場合、nullが返却されること"() {
        expect:
        HouseholdMemberConverter.toModel(null) == null
    }

    def "toModelメソッドでMHouseholdMemberからHouseholdMemberModelへマッピングされること"() {
        given: "MHouseholdMemberエンティティを準備"
        MHouseholdMember entity = new MHouseholdMember()
        entity.setHouseholdId(1L)
        entity.setUserId(10L)
        entity.setNickname("Nick")
        entity.setStatus("ACTIVE")

        when: "toModelを呼び出す"
        HouseholdMemberModel model = HouseholdMemberConverter.toModel(entity)

        then: "期待通りのマッピングが行われること"
        model.getHouseholdId() == 1L
        model.getUserId() == 10L
        model.getNickname() == "Nick"
        model.getStatus() == "ACTIVE"
        // 無視されるフィールドの確認
        model.getDisplayName() == null
        model.getProfileImageKey() == null
        model.getIconUrl() == null
        model.getRole() == null
    }
}
