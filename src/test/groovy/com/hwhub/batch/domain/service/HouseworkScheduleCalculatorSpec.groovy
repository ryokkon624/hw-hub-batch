package com.hwhub.batch.domain.service

import com.hwhub.batch.domain.enums.Category
import com.hwhub.batch.domain.enums.RecurrenceType
import com.hwhub.batch.domain.model.Housework
import spock.lang.Specification
import java.time.LocalDate

class HouseworkScheduleCalculatorSpec extends Specification {

    def calculator = new HouseworkScheduleCalculator()

    // 共通のベース Housework を作るヘルパ
    private Housework baseWeeklyHousework(Integer weeklyDays) {
        def hw = new Housework(
                1L, 1L, "週次用", "",
                Category.CLEANING.getCode(), RecurrenceType.WEEKLY.getCode(),
                weeklyDays, null, null, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null)
        return hw
    }

    private Housework baseNthWeekdayHousework(Integer nthWeek, Integer weekday) {
        def hw = new Housework(
                1L, 1L, "週次用", "",
                Category.CLEANING.getCode(), RecurrenceType.NTH_WEEKDAY.getCode(),
                null, null, nthWeek, weekday,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null)
        return hw
    }

    private Housework baseMonthlyHousework(Integer dom) {
        def hw = new Housework(
                1L, 1L, "週次用", "",
                Category.CLEANING.getCode(), RecurrenceType.MONTHLY.getCode(),
                null, dom, null, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null)
        return hw
    }

    def "週次：毎週月・金にスケジュールされること"() {
        given:
        // m_code:0002 → 0=Sun..6=Sat
        // Calculator 側の実装は idx = javaDow % 7（Mon=1..Sun=7→0）なので
        // ビット0 = 日、ビット1 = 月, ビット5 = 金
        int sundayBit = (1)
        int mondayBit = (1 << 1)
        int fridayBit = (1 << 5)
        def hw = baseWeeklyHousework(sundayBit | mondayBit | fridayBit)

        and:
        def from = LocalDate.of(2025, 1, 1)   // Wed
        def to   = LocalDate.of(2025, 1, 14)  // Tue

        when:
        def result = calculator.calculateDates(hw, from, to)

        then:
        result == [
                LocalDate.of(2025, 1, 3),  // Fri
                LocalDate.of(2025, 1, 5),  // Son
                LocalDate.of(2025, 1, 6),  // Mon
                LocalDate.of(2025, 1, 10), // Fri
                LocalDate.of(2025, 1, 12), // Sun
                LocalDate.of(2025, 1, 13)  // Mon
        ]
    }

    def "週次：weeklyDays=#weeklyDaysの場合は日付が生成されないこと"() {
        given:
        def hw = baseWeeklyHousework(weeklyDays)

        and:
        def from = LocalDate.of(2025, 1, 1)   // Wed
        def to   = LocalDate.of(2025, 1, 14)  // Tue

        when:
        def result = calculator.calculateDates(hw, from, to)

        then:
        result == []

        where:
        weeklyDays << [null, 0]
    }

    def "第n曜日：第2火曜日が正しく計算されること"() {
        given: "第2火曜日"
        def hw = baseNthWeekdayHousework(2, 2)

        and:
        def from = LocalDate.of(2025, 1, 1)
        def to   = LocalDate.of(2025, 3, 31)

        when:
        def result = calculator.calculateDates(hw, from, to)

        then:
        result == [
                LocalDate.of(2025, 1, 14), // 2025-01 の第2火曜
                LocalDate.of(2025, 2, 11),
                LocalDate.of(2025, 3, 11),
        ]
    }

    def "第n曜日：最終日曜が正しく計算されること"() {
        given: "最終日曜日（nthWeek=5, weekday=0）"
        def hw = baseNthWeekdayHousework(5,0)

        and:
        def from = LocalDate.of(2025, 1, 1)
        def to   = LocalDate.of(2025, 3, 31)

        when:
        def result = calculator.calculateDates(hw, from, to)

        then:
        result == [
                LocalDate.of(2025, 1, 26),
                LocalDate.of(2025, 2, 23),
                LocalDate.of(2025, 3, 30),
        ]
    }

    def "第n曜日：nthWeekまたはweekdayがnullの場合は日付が生成されないこと"() {
        given:
        // nthWeek / weekday に null を含むパターンを与える
        def hw = baseNthWeekdayHousework(nthWeek, weekday)

        and:
        def from = LocalDate.of(2025, 1, 1)
        def to   = LocalDate.of(2025, 3, 31)

        when:
        def result = calculator.calculateDates(hw, from, to)

        then:
        result == []

        where:
        nthWeek | weekday
        null    | 2      // 第?火曜（nthWeek 不明）
        2       | null   // 第2?曜日（weekday 不明）
        null    | null   // 両方不明
    }

    def "月次：毎月15日が範囲内で生成されること"() {
        given:
        def hw = baseMonthlyHousework(15)

        and:
        def from = LocalDate.of(2025, 1, 10)
        def to   = LocalDate.of(2025, 3, 20)

        when:
        def result = calculator.calculateDates(hw, from, to)

        then:
        result == [
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 2, 15),
                LocalDate.of(2025, 3, 15),
        ]
    }

    def "月次：31日指定のとき、月末最終日が生成されること"() {
        given:
        def hw = baseMonthlyHousework(31)

        and:
        def from = LocalDate.of(2025, 1, 1)
        def to   = LocalDate.of(2025, 3, 31)

        when:
        def result = calculator.calculateDates(hw, from, to)

        then: "2月は31日がないので28日"
        result == [
                LocalDate.of(2025, 1, 31),
                LocalDate.of(2025, 2, 28),
                LocalDate.of(2025, 3, 31),
        ]
    }

    def "月次：dayOfMonthがnullの場合、日付が生成されないこと"() {
        given:
        def hw = baseMonthlyHousework(null)

        and:
        def from = LocalDate.of(2025, 1, 1)
        def to   = LocalDate.of(2025, 3, 31)

        when:
        def result = calculator.calculateDates(hw, from, to)

        then:
        result == []
    }
}