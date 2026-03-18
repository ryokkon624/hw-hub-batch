package com.hwhub.batch.integration.job

import com.hwhub.batch.application.service.NotificationAggregationService
import com.hwhub.batch.integration.IntegrationTestBase
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDate

class NotificationAggregationServiceIntegrationSpec extends IntegrationTestBase {

    @Autowired
    NotificationAggregationService service

    Long userId
    Long householdId

    def setup() {
        // テスト用アクターユーザー INSERT（SYSTEM_USER_ID=2 はFlywayで投入済み）
        jdbcTemplate.update("""
            INSERT INTO m_user (email, password_hash, auth_provider, display_name, locale, is_active,
                                create_user_id, create_program, update_user_id, update_program)
            VALUES ('test-notif@test.com', 'hash', 'LOCAL', 'テストユーザ', 'ja', 1,
                    2, 'TEST', 2, 'TEST')
        """)
        userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM m_user WHERE email = 'test-notif@test.com'", Long)

        // 世帯 INSERT
        jdbcTemplate.update("""
            INSERT INTO m_household (name, owner_user_id, create_user_id, create_program, update_user_id, update_program)
            VALUES ('テスト世帯', ?, 2, 'TEST', 2, 'TEST')
        """, userId)
        householdId = jdbcTemplate.queryForObject(
                "SELECT MAX(household_id) FROM m_household WHERE owner_user_id = ?", Long, userId)

        // 世帯メンバー INSERT（アクターユーザーを紐付け）
        jdbcTemplate.update("""
            INSERT INTO m_household_member (household_id, user_id, nickname, status,
                                            create_user_id, create_program, update_user_id, update_program)
            VALUES (?, ?, 'テスト', '1', 2, 'TEST', 2, 'TEST')
        """, householdId, userId)
    }

    def cleanup() {
        // 削除順: t_notification → t_notification_event → t_housework_task
        //        → 世帯メンバー → 世帯 → ユーザー
        jdbcTemplate.update("DELETE FROM t_notification WHERE household_id = ?", householdId)
        jdbcTemplate.update("DELETE FROM t_notification_event WHERE household_id = ?", householdId)
        jdbcTemplate.update("DELETE FROM t_housework_task WHERE household_id = ?", householdId)
        jdbcTemplate.update("DELETE FROM m_household_member WHERE household_id = ? AND user_id = ?", householdId, userId)
        jdbcTemplate.update("DELETE FROM m_household WHERE household_id = ?", householdId)
        jdbcTemplate.update("DELETE FROM m_user WHERE user_id = ?", userId)
    }

    // ---- ヘルパー ----

    /**
     * t_housework_task を1件INSERTして housework_task_id を返す。
     * t_housework_task.housework_id は FK 制約なしのため dummy値(999999)を使用。
     */
    private Long insertHouseworkTask() {
        jdbcTemplate.update("""
            INSERT INTO t_housework_task (household_id, housework_id, name, description, category,
                                          target_date, status,
                                          create_user_id, create_program, update_user_id, update_program)
            VALUES (?, 999999, '掃除', NULL, 'CLEAN', ?, '0', 2, 'TEST', 2, 'TEST')
        """, householdId, LocalDate.now())
        return jdbcTemplate.queryForObject(
                "SELECT MAX(housework_task_id) FROM t_housework_task WHERE household_id = ?",
                Long, householdId)
    }

    /**
     * t_notification_event を1件INSERT（PENDING）。
     * notification_type='0301'（TASK_ASSIGNED）
     * event_status='0'（PENDING）
     */
    private void insertPendingNotificationEvent(Long taskId) {
        jdbcTemplate.update("""
            INSERT INTO t_notification_event (household_id, notification_type,
                                               actor_user_id, target_user_id,
                                               entity_id, aggregation_date, occurred_at,
                                               event_status,
                                               create_user_id, create_program,
                                               update_user_id, update_program)
            VALUES (?, '0301', ?, ?, ?, ?, NOW(6), '0', 2, 'TEST', 2, 'TEST')
        """, householdId, userId, userId, taskId, LocalDate.now())
    }

    // ---- テストケース ----

    def "PENDINGイベントが集約されt_notificationが生成されること"() {
        given: "家事タスク1件・PENDINGイベント1件を準備"
        Long taskId = insertHouseworkTask()
        insertPendingNotificationEvent(taskId)

        when: "aggregateTaskAssignmentNotifications() を呼び出す"
        NotificationAggregationService.Result result = service.aggregateTaskAssignmentNotifications()

        then: "クレーム件数が1であること"
        result.claimedCount() == 1

        and: "通知が1件生成されていること"
        result.notificationsInserted() == 1

        and: "DONEになったイベントが1件であること"
        result.eventsDone() == 1

        and: "t_notification にレコードが1件生成されていること"
        jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE household_id = ?",
                Integer, householdId) == 1

        and: "t_notification_event のステータスがDONE（'2'）になっていること"
        jdbcTemplate.queryForObject(
                "SELECT event_status FROM t_notification_event WHERE household_id = ?",
                String, householdId) == '2'
    }

    def "PENDINGイベントが存在しない場合は何も生成しないこと"() {
        // t_notification_event を INSERT しない

        when: "aggregateTaskAssignmentNotifications() を呼び出す"
        NotificationAggregationService.Result result = service.aggregateTaskAssignmentNotifications()

        then: "クレーム件数が0であること"
        result.claimedCount() == 0

        and: "通知が0件であること"
        result.notificationsInserted() == 0

        and: "t_notification にレコードが生成されていないこと"
        jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE household_id = ?",
                Integer, householdId) == 0
    }
}
