package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.inquiry.InquiryMessageModel;
import com.hwhub.batch.domain.model.inquiry.InquiryModel;
import com.hwhub.batch.domain.repository.InquiryRepository;
import com.hwhub.batch.infrastructure.mybatis.converter.InquiryConverter;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.InquiryCustomMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisInquiryRepository implements InquiryRepository {

  private final InquiryCustomMapper customMapper;

  @Override
  public List<InquiryModel> findOpenList(int limit) {
    return customMapper.findOpenList(limit).stream().map(InquiryConverter::toModel).toList();
  }

  @Override
  public void insertMessage(InquiryMessageModel message, long operatorUserId, String program) {
    customMapper.insertMessage(message, operatorUserId, program);
  }

  @Override
  public void updateStatus(long inquiryId, String status, long operatorUserId, String program) {
    customMapper.updateStatus(inquiryId, status, operatorUserId, program);
  }
}
