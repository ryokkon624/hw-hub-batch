package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.domain.model.Housework;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HouseworkCustomMapper {

  List<Housework> selectActiveHousework(
      @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
