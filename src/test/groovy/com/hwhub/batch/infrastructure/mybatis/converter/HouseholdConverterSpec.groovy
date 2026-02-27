package com.hwhub.batch.infrastructure.mybatis.converter

import com.hwhub.batch.domain.model.HouseholdModel
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHousehold
import spock.lang.Specification

class HouseholdConverterSpec extends Specification {

    def "toModelメソッドでエンティティがnullの場合、nullが返却されること"() {
        expect:
        HouseholdConverter.toModel(null) == null
    }

    def "toModelメソッドでMHouseholdからHouseholdModelへマッピングされること"() {
        given: "MHouseholdエンティティを準備"
        MHousehold entity = new MHousehold()
        entity.setHouseholdId(1L)
        entity.setName("Household Name")
        entity.setOwnerUserId(10L)

        when: "toModelを呼び出す"
        HouseholdModel model = HouseholdConverter.toModel(entity)

        then: "期待通りのマッピングが行われること"
        model.getHouseholdId() == 1L
        model.getName() == "Household Name"
        model.getOwnerUserId() == 10L
    }

    def "toEntityメソッドでHouseholdModelからMHouseholdへマッピングされること"() {
        given: "HouseholdModelを準備"
        HouseholdModel model = HouseholdModel.reconstruct(1L, "Household Name", 10L)

        when: "toEntityを呼び出す"
        MHousehold entity = HouseholdConverter.toEntity(model)

        then: "期待通りのマッピングが行われること"
        entity.getHouseholdId() == 1L
        entity.getName() == "Household Name"
        entity.getOwnerUserId() == 10L
    }
}
