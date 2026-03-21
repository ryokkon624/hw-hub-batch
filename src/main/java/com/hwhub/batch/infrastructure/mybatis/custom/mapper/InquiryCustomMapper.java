package com.hwhub.batch.infrastructure.mybatis.custom.mapper;

import com.hwhub.batch.domain.model.inquiry.InquiryMessageModel;
import com.hwhub.batch.infrastructure.mybatis.custom.entity.InquiryWithMessagesEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface InquiryCustomMapper {

  /** status='00'(OPEN) の最古から limit 件を messages と一緒に取得（1クエリ） */
  List<InquiryWithMessagesEntity> findOpenList(@Param("limit") int limit);

  void insertMessage(
      @Param("message") InquiryMessageModel message,
      @Param("operatorUserId") long operatorUserId,
      @Param("program") String program);

  void updateStatus(
      @Param("inquiryId") long inquiryId,
      @Param("status") String status,
      @Param("operatorUserId") long operatorUserId,
      @Param("program") String program);
}
