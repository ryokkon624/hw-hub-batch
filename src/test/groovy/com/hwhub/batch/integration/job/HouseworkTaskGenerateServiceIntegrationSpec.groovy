package com.hwhub.batch.integration.job

import com.hwhub.batch.application.service.HouseworkTaskGenerateService
import com.hwhub.batch.integration.IntegrationTestBase
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDate

class HouseworkTaskGenerateServiceIntegrationSpec extends IntegrationTestBase {

    @Autowired
    HouseworkTaskGenerateService service

    Long userId
    Long householdId
    Long houseworkId

    def setup() {
        // テスト用ユーザ INSERT
        jdbcTemplate.update("""
            INSERT INTO m_user (email, password_hash, auth_provider, display_name, locale, is_active,
                                create_user_id, create_program, update_user_id, update_program)
            VALUES ('test-generate@test.com', 'hash', 'LOCAL', 'テストユーザ', 'ja', 1,
                    2, 'TEST', 2, 'TEST')
        """)
        userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM m_user WHERE email = 'test-generate@test.com'", Long)

        // 世帯 INSERT
        jdbcTemplate.update("""
            INSERT INTO m_household (name, owner_user_id, create_user_id, create_program, update_user_id, update_program)
            VALUES ('テスト世帯', ?, 2, 'TEST', 2, 'TEST')
        """, userId)
        householdId = jdbcTemplate.queryForObject(
                "SELECT MAX(household_id) FROM m_household WHERE owner_user_id = ?", Long, userId)

        // 世帯メンバー INSERT
        jdbcTemplate.update("""
            INSERT INTO m_household_member (household_id, user_id, nickname, status,
                                            create_user_id, create_program, update_user_id, update_program)
            VALUES (?, ?, 'テスト', '1', 2, 'TEST', 2, 'TEST')
        """, householdId, userId)
    }

    def cleanup() {
        if (houseworkId) {
            jdbcTemplate.update("DELETE FROM t_housework_task WHERE housework_id = ?", houseworkId)
            jdbcTemplate.update("DELETE FROM m_housework WHERE housework_id = ?", houseworkId)
            houseworkId = null
        }
        if (householdId && userId) {
            jdbcTemplate.update("DELETE FROM m_household_member WHERE household_id = ? AND user_id = ?", householdId, userId)
            jdbcTemplate.update("DELETE FROM m_household WHERE household_id = ?", householdId)
        }
        if (userId) {
            jdbcTemplate.update("DELETE FROM m_user WHERE user_id = ?", userId)
        }
    }

    def "週次家事のタスクが生成されること"() {
        given: "週次家事を1件INSERT（全曜日有効、今日から有効期間開始）"
        LocalDate today = LocalDate.now()
        // weekly_days=127 は Sun(1)+Mon(2)+Tue(4)+Wed(8)+Thu(16)+Fri(32)+Sat(64) = 全曜日
        jdbcTemplate.update("""
            INSERT INTO m_housework (household_id, name, description, category, recurrence_type,
                                     weekly_days, start_date, end_date,
                                     create_user_id, create_program, update_user_id, update_program)
            VALUES (?, '掃除', NULL, 'CLEAN', '1', 127, ?, '2099-12-31', 2, 'TEST', 2, 'TEST')
        """, householdId, today)
        houseworkId = jdbcTemplate.queryForObject(
                "SELECT MAX(housework_id) FROM m_housework WHERE household_id = ?", Long, householdId)

        when: "generateTasks() を呼び出す"
        int inserted = service.generateTasks()

        then: "戻り値が1以上であること"
        inserted >= 1

        and: "t_housework_task にレコードが生成されていること"
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_housework_task WHERE housework_id = ?", Integer, houseworkId)
        count >= 1

        when: "generateTasks() を再度呼び出す（重複チェック）"
        int insertedAgain = service.generateTasks()

        then: "重複してタスクが生成されないこと（戻り値が0）"
        insertedAgain == 0

        and: "タスク件数が変わらないこと"
        jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_housework_task WHERE housework_id = ?", Integer, houseworkId) == count
    }

    def "有効な家事が存在しない場合は0を返すこと"() {
        // このテストでは m_housework を INSERT しない

        when: "generateTasks() を呼び出す"
        int inserted = service.generateTasks()

        then: "戻り値が0であること"
        inserted == 0

        and: "t_housework_task にレコードが生成されていないこと"
        jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_housework_task WHERE household_id = ?", Integer, householdId) == 0
    }
}
