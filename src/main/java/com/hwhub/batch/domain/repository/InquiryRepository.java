package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.inquiry.InquiryMessageModel;
import com.hwhub.batch.domain.model.inquiry.InquiryModel;
import java.util.List;

public interface InquiryRepository {

  List<InquiryModel> findOpenList(int limit);

  void insertMessage(InquiryMessageModel message, long operatorUserId, String program);

  void updateStatus(long inquiryId, String status, long operatorUserId, String program);
}
