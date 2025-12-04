package com.hwhub.batch.application.service

import com.hwhub.batch.domain.enums.RecurrenceType
import com.hwhub.batch.domain.model.Housework
import com.hwhub.batch.domain.model.HouseworkTaskCreateParam
import com.hwhub.batch.domain.repository.HouseworkRepository
import com.hwhub.batch.domain.repository.HouseworkTaskRepository
import com.hwhub.batch.domain.service.HouseworkScheduleCalculator
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneId

class HouseworkTaskGenerateServiceSpec extends Specification{

    HouseworkScheduleCalculator calculator = Mock()
    HouseworkRepository houseworkRepository = Mock()
    HouseworkTaskRepository houseworkTaskRepository = Mock()

    def service = new HouseworkTaskGenerateService(
            calculator,
            houseworkRepository,
            houseworkTaskRepository
    )

    def setup() {
        // LocalDate.now(ZoneId.of("Asia/Tokyo"))がある。必要なら Clock 差し替え版を検討する
        // 今回は引数はワイルドカードにする前提で対応不要
    }

    def "対象の housework が存在しない場合は 0 を返し、タスクは作られないこと"() {
        given:
        houseworkRepository.findActiveHouseworks(_, _) >> []

        when:
        def result = service.generateTasks()

        then:
        result == 0
        0 * calculator._
        0 * houseworkTaskRepository._
    }

    def "WEEKLY housework で既存タスクを除外して新規日付のみ生成されること"() {
        given:
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        def hw = new Housework(
                1L,
                1L,
                "ゴミ出し",
                "",
                "GARBAGE",
                RecurrenceType.WEEKLY.code,
                2,
                null, null, null,
                today.minusDays(1),   // startDate
                today.plusDays(30),   // endDate
                10L            // defaultAssigneeUserId
        )

        // calculator が返す候補日
        def d1 = today.plusDays(1)
        def d2 = today.plusDays(8)
        def d3 = today.plusDays(15)

        when:
        def totalInserted = service.generateTasks()

        then:
        totalInserted == 2
        1 * houseworkRepository.findActiveHouseworks(_, _) >> [hw]
        1 * calculator.calculateDates(hw, _, _) >> [d1, d2, d3]
        1 * houseworkTaskRepository.findExistingTaskDates(hw.houseworkId, _) >> [d2]
        1 * houseworkTaskRepository.bulkInsertTasks(_ as List<HouseworkTaskCreateParam>) >> { params ->
            assert params*.targetDate.flatten() == [d1, d3]
            return 2
        }
        0 * _
    }

    def "MONTHLY housework で既存タスクを除外して新規日付のみ生成されること"() {
        given:
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        def hw = new Housework(
                2L,
                1L,
                "風呂掃除(月次)",
                "",
                "CLEAN",
                RecurrenceType.MONTHLY.code,
                null,          // weeklyDays
                15,            // dayOfMonth
                null, null,    // nthWeek, weekday
                today.minusDays(10),
                today.plusMonths(2),
                20L
        )

        // calculator が返す候補日（例：2ヶ月分）
        def d1 = today.plusDays(5)   // 今月 15 日相当
        def d2 = today.plusDays(35)  // 来月 15 日相当

        when:
        def totalInserted = service.generateTasks()

        then:
        totalInserted == 1
        1 * houseworkRepository.findActiveHouseworks(_, _) >> [hw]
        1 * calculator.calculateDates(hw, _, _) >> [d1, d2]
        // すでに今月分 d1 は存在、来月分 d2 だけ新規
        1 * houseworkTaskRepository.findExistingTaskDates(hw.houseworkId, _) >> [d1]
        1 * houseworkTaskRepository.bulkInsertTasks(_ as List<HouseworkTaskCreateParam>) >> { params ->
            assert params*.targetDate.flatten() == [d2]
            return 1
        }
        0 * _
    }

    def "NTH_WEEKDAY housework で候補日がない場合は何も作成されないこと"() {
        given:
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        def hw = new Housework(
                3L,
                1L,
                "第2火曜のゴミ出し",
                "",
                "GARBAGE",
                RecurrenceType.NTH_WEEKDAY.code,
                null,     // weeklyDays
                null,     // dayOfMonth
                2,        // nthWeek = 2
                2,        // weekday = 火曜
                today.plusDays(1),
                today.plusMonths(1),
                30L
        )

        def d1 = today.plusDays(5)   // 今月 15 日相当

        when:
        def totalInserted = service.generateTasks()

        then:
        totalInserted == 0
        1 * houseworkRepository.findActiveHouseworks(_, _) >> [hw]
        // 日付生成ロジックの結果が空のケース
        1 * calculator.calculateDates(hw, _, _) >> [d1]
        1 * houseworkTaskRepository.findExistingTaskDates(_, _) >> [d1]
        0 * houseworkTaskRepository.bulkInsertTasks(_)
        0 * _
    }

    def "WEEKLY 有効期間が範囲外の場合に何も生成されないこと"() {
        given:
        def today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        def hw = new Housework(
                1L,
                1L,
                "ゴミ出し",
                "",
                "GARBAGE",
                RecurrenceType.WEEKLY.code,
                2,
                null, null, null,
                today.minusDays(20),   // startDate
                today.minusDays(1),   // endDate
                10L            // defaultAssigneeUserId
        )

        when:
        def totalInserted = service.generateTasks()

        then:
        totalInserted == 0
        1 * houseworkRepository.findActiveHouseworks(_, _) >> [hw]
        0 * calculator.calculateDates(hw, _, _)
        0 * houseworkTaskRepository.findExistingTaskDates(hw.houseworkId, _)
        0 * houseworkTaskRepository.bulkInsertTasks(_ as List<HouseworkTaskCreateParam>)
        0 * _
    }
}
