package com.hwhub.batch.infrastructure.mybatis.generated.mapper;

import com.hwhub.batch.infrastructure.mybatis.generated.entity.MCode;
import com.hwhub.batch.infrastructure.mybatis.generated.entity.MCodeExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MCodeMapper {
    int deleteByPrimaryKey(@Param("codeType") String codeType, @Param("codeValue") String codeValue);

    int insert(MCode row);

    int insertSelective(MCode row);

    List<MCode> selectByExample(MCodeExample example);

    MCode selectByPrimaryKey(@Param("codeType") String codeType, @Param("codeValue") String codeValue);

    int updateByPrimaryKeySelective(MCode row);

    int updateByPrimaryKey(MCode row);
}