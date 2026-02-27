package com.hwhub.batch.application.service

import com.hwhub.batch.domain.enums.ProgramType
import com.hwhub.batch.domain.repository.HouseholdRepository
import spock.lang.Specification

class HouseholdCleanupServiceSpec extends Specification {

    HouseholdCleanupService service
    HouseholdRepository repository = Mock(HouseholdRepository)

    def setup() {
        service = new HouseholdCleanupService(repository)
    }

    def "cleanupHouseholds: リポジトリを呼び出し、削除件数を返す"() {
        given:
        int expectedDeleted = 5

        when:
        int actual = service.cleanupHouseholds()

        then:
        1 * repository.deleteOrphanedHouseholds(_, ProgramType.BTC_HLD_CLEN.code) >> expectedDeleted
        actual == expectedDeleted
    }
}
