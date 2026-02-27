package com.hwhub.batch.infrastructure.mybatis.converter

import com.hwhub.batch.domain.model.UserModel
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MUser
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId

class UserConverterSpec extends Specification {

    def "toModelメソッドでエンティティがnullの場合、nullが返却されること"() {
        expect:
        UserConverter.toModel(null) == null
    }

    def "toModelメソッドでMUserからUserModelへマッピングされること"() {
        given: "MUserエンティティを準備"
        LocalDateTime pwChangedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
        LocalDateTime emailVrfAt = LocalDateTime.of(2023, 1, 2, 10, 0)

        MUser entity = new MUser()
        entity.setUserId(1L)
        entity.setEmail("test@ex.com")
        entity.setPasswordHash("hash")
        entity.setPasswordChangedAt(java.util.Date.from(pwChangedAt.atZone(ZoneId.systemDefault()).toInstant()))
        entity.setAuthProvider("local")
        entity.setAuthProviderId("id")
        entity.setDisplayName("Name")
        entity.setLocale("ja")
        entity.setProfileImageKey("key")
        entity.setEmailVerifiedAt(java.util.Date.from(emailVrfAt.atZone(ZoneId.systemDefault()).toInstant()))
        entity.setIsActive(true)

        when: "toModelを呼び出す"
        UserModel model = UserConverter.toModel(entity)

        then: "期待通りのマッピングが行われること"
        model.getUserId() == 1L
        model.getEmail() == "test@ex.com"
        model.getPasswordHash() == "hash"
        model.getPasswordChangedAt() == pwChangedAt
        model.getAuthProvider() == "local"
        model.getAuthProviderId() == "id"
        model.getDisplayName() == "Name"
        model.getLocale() == "ja"
        model.getProfileImageKey() == "key"
        model.getEmailVerifiedAt() == emailVrfAt
        model.isActive()
    }
}
