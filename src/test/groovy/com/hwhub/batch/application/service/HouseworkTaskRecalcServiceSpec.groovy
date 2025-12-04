package com.hwhub.batch.application.service

import com.hwhub.batch.domain.enums.ProgramType
import com.hwhub.batch.domain.enums.RecurrenceType;
import com.hwhub.batch.domain.model.Housework
import com.hwhub.batch.domain.model.HouseworkTaskCreateParam
import  com.hwhub.batch.domain.model.HouseworkTaskRecalcRequest
import com.hwhub.batch.domain.repository.HouseworkRepository
import com.hwhub.batch.domain.repository.HouseworkTaskRecalcRequestRepository
import com.hwhub.batch.domain.repository.HouseworkTaskRepository
import com.hwhub.batch.domain.service.HouseworkScheduleCalculator
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneId;

class HouseworkTaskRecalcServiceSpec extends Specification{

    HouseworkScheduleCalculator calculator = Mock()
    HouseworkTaskRecalcRequestRepository requestRepository = Mock()
    HouseworkTaskRepository taskRepository = Mock()
    HouseworkRepository houseworkRepository = Mock()

    def service = new HouseworkTaskRecalcService(
            calculator,
            requestRepository,
            taskRepository,
            houseworkRepository
    )

    def "PENDINGの再計算リクエストが存在しない場合は0を返し、他の処理が行われないこと"() {
        given:
        int limit = 100

        when:
        def result = service.recalcPending(limit)

        then:
        result == 0
        1 * requestRepository.findPendingRequests(limit) >> []
        0 * houseworkRepository._
        0 * taskRepository._
        0 * calculator._
    }

    def "1つのhouseworkに対するPENDINGリクエストを再計算しDONEに更新すること"() {
        given:
        int limit = 100
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        long houseworkId = 10L
        long requestId = 1L

        def req = new HouseworkTaskRecalcRequest(1L, 10L)

        def hw = new Housework(
                houseworkId,
                1L,
                "週次ゴミ出し",
                "",
                "GARBAGE",
                RecurrenceType.WEEKLY.code,
                2,      // weeklyDays（具体値はどうでもよい）
                null, null, null,
                today.minusDays(1),
                today.plusDays(30),
                20L     // defaultAssigneeUserId
        )

        def d1 = today.plusDays(1)
        def d2 = today.plusDays(8)
        def d3 = today.plusDays(15)

        when:
        def totalAffected = service.recalcPending(limit)

        then:
        totalAffected == 4  // snapshot(1) + assignee(1) + deleted(0) + inserted(2) = 4

        // PENDING リクエスト取得
        1 * requestRepository.findPendingRequests(limit) >> [req]
        // houseworkIdごとに処理 → まず Housework 取得
        1 * houseworkRepository.findById(houseworkId) >> Optional.of(hw)
        // 期待日付の計算
        1 * calculator.calculateDates(hw, _, _) >> [d1, d2, d3]
        // 既存タスク（日付）は today 以降
        1 * taskRepository.findExistingTaskDates(houseworkId, _) >> [d1]
        // snapshot 更新
        1 * taskRepository.updateTaskSnapshot(
                houseworkId,
                _,
                hw.name,
                hw.description,
                hw.category,
                _,
                _
        ) >> 1
        // defaultAssigneeUserId があるので assignee 更新
        1 * taskRepository.updateAssigneeForSystemAssigned(
                houseworkId,
                _,
                hw.defaultAssigneeUserId,
                _,
                _
        ) >> 1
        // 削除対象は無し（既存＝[d1], 期待＝[d1,d2,d3]）
        1 * taskRepository.deleteUndoneTasksByDates(houseworkId, _) >> { Long id, Set<LocalDate> dates ->
            assert dates.isEmpty()
            return 0
        }
        // d2, d3 の2件を insert
        1 * taskRepository.bulkInsertTasks(_ as List<HouseworkTaskCreateParam>) >> { List<HouseworkTaskCreateParam> params ->
            assert params*.targetDate.flatten() as Set == [d2, d3] as Set
            return 2
        }
        // 成功した request は DONE
        1 * requestRepository.markDoneByRequestIds([requestId], 2, ProgramType.BTC_TSK_RECL.getCode())
        0 * _
    }

    def "houseworkにデフォルト担当者がない場合に更新処理を行わないこと"() {
        given:
        int limit = 100
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        long houseworkId = 10L
        long requestId = 1L

        def req = new HouseworkTaskRecalcRequest(1L, 10L)

        def hw = new Housework(
                houseworkId,
                1L,
                "週次ゴミ出し",
                "",
                "GARBAGE",
                RecurrenceType.WEEKLY.code,
                2,      // weeklyDays（具体値はどうでもよい）
                null, null, null,
                today.minusDays(1),
                today.plusDays(30),
                null     // defaultAssigneeUserId
        )

        def d1 = today.plusDays(1)
        def d2 = today.plusDays(8)
        def d3 = today.plusDays(15)

        when:
        def totalAffected = service.recalcPending(limit)

        then:
        totalAffected == 3  // snapshot(1) + assignee(0) + deleted(0) + inserted(2) = 4

        // PENDING リクエスト取得
        1 * requestRepository.findPendingRequests(limit) >> [req]
        // houseworkIdごとに処理 → まず Housework 取得
        1 * houseworkRepository.findById(houseworkId) >> Optional.of(hw)
        // 期待日付の計算
        1 * calculator.calculateDates(hw, _, _) >> [d1, d2, d3]
        // 既存タスク（日付）は today 以降
        1 * taskRepository.findExistingTaskDates(houseworkId, _) >> [d1]
        // snapshot 更新
        1 * taskRepository.updateTaskSnapshot(
                houseworkId,
                _,
                hw.name,
                hw.description,
                hw.category,
                _,
                _
        ) >> 1
        // defaultAssigneeUserId がないため更新しない
        0 * taskRepository.updateAssigneeForSystemAssigned()
        // 削除対象は無し（既存＝[d1], 期待＝[d1,d2,d3]）
        1 * taskRepository.deleteUndoneTasksByDates(houseworkId, _) >> { Long id, Set<LocalDate> dates ->
            assert dates.isEmpty()
            return 0
        }
        // d2, d3 の2件を insert
        1 * taskRepository.bulkInsertTasks(_ as List<HouseworkTaskCreateParam>) >> { List<HouseworkTaskCreateParam> params ->
            assert params*.targetDate.flatten() as Set == [d2, d3] as Set
            return 2
        }
        // 成功した request は DONE
        1 * requestRepository.markDoneByRequestIds([requestId], 2, ProgramType.BTC_TSK_RECL.getCode())
        0 * _
    }

    def "Houseworkが存在しない場合はmarkFailedByRequestIdsが呼ばれること"() {
        given:
        int limit = 100
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        long houseworkId = 99L
        long requestId = 10L

        def req = new HouseworkTaskRecalcRequest(requestId, houseworkId)

        when:
        def totalAffected = service.recalcPending(limit)

        then:
        // 例外はrecalcPending内でcatchされるので0戻り
        totalAffected == 0

        and:
        1 * requestRepository.findPendingRequests(limit) >> [req]
        // Housework が見つからず Optional.empty()
        1 * houseworkRepository.findById(houseworkId) >> Optional.empty()
        // markDone は呼ばれない
        0 * requestRepository.markDoneByRequestIds(_, _, _)
        // 代わりに markFailed が呼ばれる
        1 * requestRepository.markFailedByRequestIds(
                [requestId],
{ String msg -> msg.contains("Housework not found") }, 2, ProgramType.BTC_TSK_RECL.getCode()
        )
        0 * _
    }

    def "有効期間fromがtoより後になる場合は何も更新されないこと"() {
        given:
        int limit = 100
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        long houseworkId = 20L
        long requestId = 200L

        def req = new HouseworkTaskRecalcRequest(requestId, houseworkId)

        // WEEKLY だが startDate を today+10 にしておく
        def hw = new Housework(
                houseworkId,
                1L,
                "未来日からの週次タスク",
                "",
                "OTHER",
                RecurrenceType.WEEKLY.code,
                2,
                null, null, null,
                today.plusDays(10),  // startDate
                today.plusDays(20),  // endDate（rangeTo は today+7 なので from>to になる）
                null                 // defaultAssigneeUserId
        )

        when:
        def totalAffected = service.recalcPending(limit)

        then:
        totalAffected == 0

        and:
        1 * requestRepository.findPendingRequests(limit) >> [req]
        1 * houseworkRepository.findById(houseworkId) >> Optional.of(hw)
        // 範囲なしのため、calculateDates もタスク系も呼ばれない
        0 * calculator.calculateDates(_, _, _)
        0 * taskRepository.updateTaskSnapshot(_, _, _, _, _, _, _)
        0 * taskRepository.updateAssigneeForSystemAssigned(_, _, _, _, _)
        0 * taskRepository.deleteUndoneTasksByDates(_, _)
        0 * taskRepository.bulkInsertTasks(_)
        1 * requestRepository.markDoneByRequestIds([requestId], 2, ProgramType.BTC_TSK_RECL.getCode())
        0 * _
    }

    def "期待する日付がない場合は何も更新されないこと"() {
        given:
        int limit = 100
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        long houseworkId = 20L
        long requestId = 200L

        def req = new HouseworkTaskRecalcRequest(requestId, houseworkId)

        // WEEKLY だが startDate を today+10 にしておく
        def hw = new Housework(
                houseworkId,
                1L,
                "短期間",
                "",
                "OTHER",
                RecurrenceType.MONTHLY.code,
                null,
                15, null, null,
                today.minusDays(10),  // startDate
                today.plusDays(10),  // endDate
                null
        )

        when:
        def totalAffected = service.recalcPending(limit)

        then:
        totalAffected == 0

        and:
        1 * requestRepository.findPendingRequests(limit) >> [req]
        1 * houseworkRepository.findById(houseworkId) >> Optional.of(hw)
        1 * calculator.calculateDates(_, _, _) >> []
        0 * taskRepository.updateTaskSnapshot(_, _, _, _, _, _, _)
        0 * taskRepository.updateAssigneeForSystemAssigned(_, _, _, _, _)
        0 * taskRepository.deleteUndoneTasksByDates(_, _)
        0 * taskRepository.bulkInsertTasks(_)
        1 * requestRepository.markDoneByRequestIds([requestId], 2, ProgramType.BTC_TSK_RECL.getCode())
        0 * _
    }

}
