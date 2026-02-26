package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.infrastructure.mybatis.generated.entity.MUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserCustomMapper {

  List<MUser> findByIds(@Param("userIds") List<Long> userIds);
}
