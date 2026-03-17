package com.hwhub.batch.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class SpringContextIntegrationSpec extends IntegrationTestBase {

    @Autowired
    ApplicationContext applicationContext

    def "Springコンテキストが正常に起動すること"() {
        expect:
        applicationContext != null
    }
}
