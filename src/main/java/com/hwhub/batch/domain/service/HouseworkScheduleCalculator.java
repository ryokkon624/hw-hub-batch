package com.hwhub.batch.domain.service;

import com.hwhub.batch.domain.enums.RecurrenceType;
import com.hwhub.batch.domain.model.Housework;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HouseworkScheduleCalculator {

  private static final Logger log = LoggerFactory.getLogger(HouseworkScheduleCalculator.class);

  /**
   * 指定された家事モデルから生成されるタスクの日付を計算して返す。
   *
   * @param housework 家事モデル
   * @param from 日付From
   * @param to 日付To
   * @return タスクを生成する日付
   */
  public List<LocalDate> calculateDates(Housework housework, LocalDate from, LocalDate to) {
    RecurrenceType type = RecurrenceType.fromCode(housework.getRecurrenceTypeCode());
    return switch (type) {
      case WEEKLY -> generateWeeklyDates(housework, from, to);
      case MONTHLY -> generateMonthlyDates(housework, from, to);
      case NTH_WEEKDAY -> generateNthWeekdayDates(housework, from, to);
    };
  }

  /**
   * Weekly: weekly_days ビットマスクに従い from〜to の日付を返す。 weekly_days のビット割り当て: bit0=Sun, bit1=Mon, ...,
   * bit6=Sat。
   *
   * @param hw Housework
   * @param from 対象期間From
   * @param to 対象期間To
   * @return 生成するTaskのTarget Dateのリスト
   */
  private List<LocalDate> generateWeeklyDates(Housework hw, LocalDate from, LocalDate to) {
    Integer bitMask = hw.getWeeklyDays();
    if (bitMask == null || bitMask == 0) {
      return List.of();
    }

    log.debug(
        "GenerateWeeklyDates start houseworkId={}, from={}, to={}", hw.getHouseworkId(), from, to);

    List<LocalDate> dates = new ArrayList<>();
    for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
      // DayOfWeek: Mon:1..Sun=7
      int dow = d.getDayOfWeek().getValue();
      // 0:Sun, 1:Mon, ...6:Satにマッピング
      int idx = dow % 7;
      if ((bitMask & (1 << idx)) != 0) {
        dates.add(d);
      }
    }
    log.debug("GenerateWeeklyDates end candidateDate={}", dates);
    return dates;
  }

  /**
   * Monthly: day_of_month に従い from〜to の範囲に入る日付を返す。 day_Of_Monthが31の場合は月末最終日を表す。
   *
   * @param hw Housework
   * @param from 対象期間From
   * @param to 対象期間To
   * @return 生成するTaskのTarget Dateのリスト
   */
  private List<LocalDate> generateMonthlyDates(Housework hw, LocalDate from, LocalDate to) {
    Integer dayOfMonth = hw.getDayOfMonth();
    if (dayOfMonth == null) {
      return List.of();
    }
    log.debug(
        "GenerateMonthlyDates start houseworkId={}, from={}, to={}", hw.getHouseworkId(), from, to);

    List<LocalDate> dates = new ArrayList<>();
    // 開始月の1日
    LocalDate cursor = from.withDayOfMonth(1);
    while (!cursor.isAfter(to)) {
      int lom = cursor.lengthOfMonth();

      if (dayOfMonth <= lom || dayOfMonth == 31) {
        LocalDate candidate;
        if (dayOfMonth == 31) {
          // 31の場合はその月の月末最終日
          candidate = cursor.withDayOfMonth(lom);
        } else {
          candidate = cursor.withDayOfMonth(dayOfMonth);
        }
        if (!candidate.isBefore(from) && !candidate.isAfter(to)) {
          dates.add(candidate);
        }
      }
      cursor = cursor.plusMonths(1);
    }
    log.debug("GenerateMonthlyDates end candidateDate={}", dates);
    return dates;
  }

  /**
   * NthWeekday: nth_week + weekday に従い from〜to の範囲に入る「第n○曜日 / 最終○曜日」を返す。 nth_week: 1～4、5=最終週
   * weekday: 0:SUn, 1:Mon, ... 6=Sat
   *
   * @param hw Housework
   * @param from 対象期間From
   * @param to 対象期間To
   * @return 生成するTaskのTarget Dateのリスト
   */
  private List<LocalDate> generateNthWeekdayDates(Housework hw, LocalDate from, LocalDate to) {
    Integer nthWeek = hw.getNthWeek();
    Integer weekday = hw.getWeekday();
    if (nthWeek == null || weekday == null) {
      return List.of();
    }
    log.debug(
        "GenerateNthWeekdayDates start houseworkId={}, from={}, to={}",
        hw.getHouseworkId(),
        from,
        to);

    List<LocalDate> dates = new ArrayList<>();
    LocalDate cursor = from.withDayOfMonth(1);
    while (!cursor.isAfter(to)) {
      LocalDate candidate = nthWeekdayOfMonth(cursor, weekday, nthWeek);

      if (!candidate.isBefore(from) && !candidate.isAfter(to)) {
        dates.add(candidate);
      }

      cursor = cursor.plusMonths(1);
    }
    log.debug("GenerateNthWeekdayDates end candidateDate={}", dates);

    return dates;
  }

  /**
   * 指定した月の第n週(nthWeek)の曜日に該当する日付を返す。
   *
   * @param date 対象の日付
   * @param weekday Modelの曜日(0:Sun, 1:Mon, ... 6:Sat)
   * @param nthWeek 第n週(1: 第1週, 2:第2週, ... 5:最終週)
   * @return 指定した月の第n週の曜日に該当する日付
   */
  private LocalDate nthWeekdayOfMonth(LocalDate date, Integer weekday, Integer nthWeek) {
    // 最終週が指定されている場合
    if (nthWeek == 5) {
      return lastWeekdayOfMonth(date, weekday);
    }

    LocalDate cursor = date.withDayOfMonth(1);
    int found = 0;
    DayOfWeek dayOfWeek = weekdayToJavaDayOfWeek(weekday);

    while (cursor.getMonth() == date.getMonth()) {
      if (cursor.getDayOfWeek().equals(dayOfWeek)) {
        found++;
        if (found == nthWeek) {
          return cursor;
        }
      }
      cursor = cursor.plusDays(1);
    }

    // 特定の曜日が月に4日存在しないことはあり得ない。月末を返す。
    return cursor;
  }

  /**
   * 指定した月の最終週のweekday(0:Sun, ... 6:Sat)の日付を返す。
   *
   * @param date 対象の日付
   * @param weekday Modelの曜日(0:Sun, 1:Mon, ... 6:Sat)
   * @return 指定した月の最終週のweekdayの日付
   */
  private LocalDate lastWeekdayOfMonth(LocalDate date, Integer weekday) {
    LocalDate lastDay = date.withDayOfMonth(date.lengthOfMonth());
    DayOfWeek dow = weekdayToJavaDayOfWeek(weekday);

    LocalDate cursor = lastDay;
    while (cursor.getMonth() == date.getMonth()) {
      if (cursor.getDayOfWeek().equals(dow)) {
        return cursor;
      }
      cursor = cursor.minusDays(1);
    }

    // ある月に該当の曜日が存在しないことはあり得ない
    return cursor;
  }

  /**
   * Modelの曜日のコード(0:Sun, ... 6:Sat)からjava.time.DayOfWeekのgetValue()に変換して返す。
   *
   * @param weekday Modelの曜日(0:Sun, 1:Mon, ... 6:Sat)
   * @return java.time.DayOfWeekのgetValue()の値
   */
  private DayOfWeek weekdayToJavaDayOfWeek(Integer weekday) {
    if (weekday == 0) {
      return DayOfWeek.of(7);
    }
    return DayOfWeek.of(weekday);
  }
}
