package com.hwhub.batch.infrastructure.mybatis.repository;

import com.hwhub.batch.domain.model.HouseworkTaskRecalcRequest;
import com.hwhub.batch.domain.repository.HouseworkTaskRecalcRequestRepository;
import com.hwhub.batch.infrastructure.mybatis.custom.mapper.HouseworkTaskRecalcRequestCustomMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisHouseworkTaskRecalcRequestRepository
    implements HouseworkTaskRecalcRequestRepository {

  private final HouseworkTaskRecalcRequestCustomMapper customMapper;

  /**
   * PENDING のリクエストを行単位で取得する。
   *
   * @param limit 検索上限
   * @return PENDING のリクエストリスト
   */
  @Override
  public List<HouseworkTaskRecalcRequest> findPendingRequests(int limit) {
    return customMapper.selectPendingRequests(limit);
  }

  /**
   * 指定されたrequest_idのステータスをDONEに更新する。
   *
   * @param requestIds request_idリスト
   * @param userId 更新ユーザのユーザID
   * @param programName プログラム名
   */
  @Override
  public void markDoneByRequestIds(List<Long> requestIds, Long userId, String programName) {
    customMapper.updateStatusToDoneByRequestIds(requestIds, userId, programName);
  }

  /**
   * 指定されたrequest_idのステータスをFAILEDに更新する。
   *
   * @param requestIds request_idリスト
   * @param errorMessage エラーメッセージ
   * @param userId 更新ユーザのユーザID
   * @param programName プログラム名
   */
  @Override
  public void markFailedByRequestIds(
      List<Long> requestIds, String errorMessage, Long userId, String programName) {
    customMapper.updateStatusToFailedByRequestIds(requestIds, errorMessage, userId, programName);
  }
}
