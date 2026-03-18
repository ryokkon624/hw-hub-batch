package com.hwhub.batch.integration.job

import com.hwhub.batch.application.service.HouseworkTaskRecalcService
import com.hwhub.batch.integration.IntegrationTestBase
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDate

class HouseworkTaskRecalcServiceIntegrationSpec extends IntegrationTestBase {

    @Autowired
    HouseworkTaskRecalcService service

    Long userId
    Long householdId
    Long houseworkId

    def setup() {
        jdbcTemplate.update("""
            INSERT INTO m_user (email, password_hash, auth_provider, display_name, locale, is_active,
                                create_user_id, create_program, update_user_id, update_program)
            VALUES ('test-recalc@test.com', 'hash', 'LOCAL', 'テストユーザ', 'ja', 1,
                    2, 'TEST', 2, 'TEST')
        """)
        userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM m_user WHERE email = 'test-recalc@test.com'", Long)

        jdbcTemplate.update("""
            INSERT INTO m_household (name, owner_user_id, create_user_id, create_program, update_user_id, update_program)
            VALUES ('テスト世帯', ?, 2, 'TEST', 2, 'TEST')
        """, userId)
        householdId = jdbcTemplate.queryForObject(
                "SELECT MAX(household_id) FROM m_household WHERE owner_user_id = ?", Long, userId)

        jdbcTemplate.update("""
            INSERT INTO m_household_member (household_id, user_id, nickname, status,
                                            create_user_id, create_program, update_user_id, update_program)
            VALUES (?, ?, 'テスト', '1', 2, 'TEST', 2, 'TEST')
        """, householdId, userId)
    }

    def cleanup() {
        if (houseworkId) {
            jdbcTemplate.update("DELETE FROM t_housework_task_recalc_request WHERE housework_id = ?", houseworkId)
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

    // ---- ヘルパー ----

    private Long insertHousework(int weeklyDays) {
        jdbcTemplate.update("""
            INSERT INTO m_housework (household_id, name, description, category, recurrence_type,
                                     weekly_days, start_date, end_date,
                                     create_user_id, create_program, update_user_id, update_program)
            VALUES (?, '掃除', NULL, 'CLEAN', '1', ?, ?, '2099-12-31', 2, 'TEST', 2, 'TEST')
        """, householdId, weeklyDays, LocalDate.now())
        return jdbcTemplate.queryForObject(
                "SELECT MAX(housework_id) FROM m_housework WHERE household_id = ?", Long, householdId)
    }

    private void insertTask(Long hwId, LocalDate targetDate) {
        jdbcTemplate.update("""
            INSERT INTO t_housework_task (household_id, housework_id, name, description, category,
                                          target_date, status,
                                          create_user_id, create_program, update_user_id, update_program)
            VALUES (?, ?, '掃除', NULL, 'CLEAN', ?, '0', 2, 'TEST', 2, 'TEST')
        """, householdId, hwId, targetDate)
    }

    private void insertPendingRequest(Long hwId) {
        jdbcTemplate.update("""
            INSERT INTO t_housework_task_recalc_request (housework_id, recalc_request_status,
                                                          create_user_id, create_program,
                                                          update_user_id, update_program)
            VALUES (?, '0', 2, 'TEST', 2, 'TEST')
        """, hwId)
    }

    // ---- テストケース ----

    def "PENDINGのリクエストが処理されDONEになること"() {
        given: "週次家事・タスク1件・PENDINGリクエストを準備"
        LocalDate today = LocalDate.now()
        houseworkId = insertHousework(127) // weekly_days=127: 全曜日
        insertTask(houseworkId, today)
        insertPendingRequest(houseworkId)

        when: "recalcPending(10) を呼び出す"
        int affected = service.recalcPending(10)

        then: "戻り値が0以上であること"
        affected >= 0

        and: "t_housework_task_recalc_request のステータスがDONE（'2'）になっていること"
        jdbcTemplate.queryForObject(
                "SELECT recalc_request_status FROM t_housework_task_recalc_request WHERE housework_id = ?",
                String, houseworkId) == '2'
    }

    def "PENDINGリクエストが存在しない場合は0を返すこと"() {
        // t_housework_task_recalc_request を INSERT しない

        when: "recalcPending(10) を呼び出す"
        int affected = service.recalcPending(10)

        then: "戻り値が0であること"
        affected == 0
    }

    def "家事マスタが変更された場合タスクが再生成されること"() {
        given: "3日後の曜日を除外したweekly_daysで家事を作成し、その日付のタスクを1件手動INSERT"
        LocalDate today = LocalDate.now()
        LocalDate targetDate = today.plusDays(3)

        // today+3 の曜日ビットを計算（weekly_days: Sun=bit0, Mon=bit1, ..., Sat=bit6）
        // DayOfWeek.getValue(): Mon=1..Sun=7 → idx = dow % 7 で Sun=0, Mon=1, ..., Sat=6
        int idx = targetDate.getDayOfWeek().getValue() % 7
        int bitForTargetDay = 1 << idx
        int weeklyDaysExcludingTarget = 127 - bitForTargetDay // today+3 の曜日のみ除外

        houseworkId = insertHousework(weeklyDaysExcludingTarget)

        // today+3 は weekly_days に含まれない曜日のタスクを手動INSERT（削除対象）
        insertTask(houseworkId, targetDate)

        int countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_housework_task WHERE housework_id = ? AND target_date = ?",
                Integer, houseworkId, targetDate)

        insertPendingRequest(houseworkId)

        when: "recalcPending(10) を呼び出す"
        service.recalcPending(10)

        then: "手動INSERTした today+3 のタスクが削除されていること"
        countBefore == 1
        jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_housework_task WHERE housework_id = ? AND target_date = ?",
                Integer, houseworkId, targetDate) == 0

        and: "リクエストがDONE（'2'）になっていること"
        jdbcTemplate.queryForObject(
                "SELECT recalc_request_status FROM t_housework_task_recalc_request WHERE housework_id = ?",
                String, houseworkId) == '2'
    }
}
