package com.hwhub.batch.application.service

import com.hwhub.batch.domain.repository.HouseholdInvitationRepository
import spock.lang.Specification

class InvitationExpireServiceSpec extends Specification {

    HouseholdInvitationRepository repository = Mock()

    def InvitationExpireService service = new InvitationExpireService(repository)

    def "リポジトリを呼び出しているか"() {
        when:
            def result = service.expireInvitations()

        then:
        result == 2
        1 * repository.expirePendingInvitations(_, _) >> 2
    }
}
