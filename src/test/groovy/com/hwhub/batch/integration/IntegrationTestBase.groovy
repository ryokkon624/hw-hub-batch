package com.hwhub.batch.integration

import com.hwhub.batch.HwHubBatchApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.MySQLContainer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Tag

@SpringBootTest(
    classes = [HwHubBatchApplication],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Tag("integration")
abstract class IntegrationTestBase extends Specification {

    @Shared
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("hwhub_db")
            .withUsername("hwhub")
            .withPassword("hwhub")
            .tap { container ->
                container.start()
                // Springコンテキスト起動前にシステムプロパティで注入
                System.setProperty("spring.datasource.url", container.jdbcUrl)
                System.setProperty("spring.datasource.username", container.username)
                System.setProperty("spring.datasource.password", container.password)
            }

    @Autowired
    protected JdbcTemplate jdbcTemplate
}
