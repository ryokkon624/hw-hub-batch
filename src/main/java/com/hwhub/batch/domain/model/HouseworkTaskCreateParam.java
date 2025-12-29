package com.hwhub.batch.domain.model;

import com.hwhub.batch.domain.enums.TaskAssignReason;
import com.hwhub.batch.domain.enums.TaskStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HouseworkTaskCreateParam {
  private final Long householdId;
  private final Long houseworkId;
  private final String name;
  private final String description;
  private final String category;
  private final LocalDate targetDate;
  private final Long assigneeUserId;
  private final String statusCode;
  private final String assignReasonType;
  private final Long createUserId;
  private final String createProgram;
  private final Long updateUserId;
  private final String updateProgram;

  public static HouseworkTaskCreateParam from(
      Housework housework, LocalDate targetDate, long opeUserId, String program) {
    return new HouseworkTaskCreateParam(
        housework.getHouseholdId(),
        housework.getHouseworkId(),
        housework.getName(),
        housework.getDescription(),
        housework.getCategory(),
        targetDate,
        housework.getDefaultAssigneeUserId(),
        TaskStatus.NOT_DONE.getCode(),
        TaskAssignReason.SYSTEM_ASSIGNED.getCode(), // m_code:0005 「未対応」
        opeUserId,
        program,
        opeUserId,
        program);
  }
}
