package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.Housework;
import com.hwhub.batch.domain.repository.HouseworkRepository;
import com.hwhub.batch.infrastructure.mybatis.converter.DateConverter;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.HouseworkCustomMapper;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MHousework;
import com.hwhub.batch.infrastructure.mybatis.generated.mapper.MHouseworkMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisHouseworkRepository implements HouseworkRepository {

  private final MHouseworkMapper mapper;
  private final HouseworkCustomMapper customMapper;

  /**
   * 有効期間がfrom、toと交差している家事マスタを取得する。
   *
   * @param from From
   * @param to To
   * @return 有効期間がfrom、toと交差している家事マスタ
   */
  @Override
  public List<Housework> findActiveHouseworks(LocalDate from, LocalDate to) {
    return customMapper.selectActiveHousework(from, to);
  }

  /**
   * 指定された家事IDの家事マスタを取得する。
   *
   * @param houseworkId 家事ID
   * @return 指定された家事IDの家事マスタ
   */
  @Override
  public Optional<Housework> findById(Long houseworkId) {
    MHousework entity = mapper.selectByPrimaryKey(houseworkId);

    return Optional.of(
        new Housework(
            entity.getHouseworkId(),
            entity.getHouseholdId(),
            entity.getName(),
            entity.getDescription(),
            entity.getCategory(),
            entity.getRecurrenceType(),
            entity.getWeeklyDays(),
            entity.getDayOfMonth(),
            entity.getNthWeek(),
            entity.getWeekday(),
            DateConverter.toLocalDate(entity.getStartDate()),
            DateConverter.toLocalDate(entity.getEndDate()),
            entity.getDefaultAssigneeUserId()));
  }
}
