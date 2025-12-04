package com.hwhub.batch.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class Housework {
    private final Long houseworkId;
    private final Long householdId;
    private final String name;
    private final String description;
    private final String category;
    private final String recurrenceTypeCode;
    private final Integer weeklyDays;       // ビットマスク（null許容）
    private final Integer dayOfMonth;       // 月次実行日
    private final Integer nthWeek;          // 第n週
    private final Integer weekday;          // 0:Sun〜6:Sat （m_code:0002）
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Long defaultAssigneeUserId;

}
