package com.hwhub.batch.domain.repository;

import com.hwhub.batch.domain.model.HouseworkTaskRecalcRequest;
import java.util.List;

public interface HouseworkTaskRecalcRequestRepository {

  /**
   * PENDING のリクエストを行単位で取得する。
   *
   * @param limit 検索上限
   * @return PENDING のリクエストリスト
   */
  List<HouseworkTaskRecalcRequest> findPendingRequests(int limit);

  /**
   * 指定されたrequest_idのステータスをDONEに更新する。
   *
   * @param requestIds request_idリスト
   * @param userId 更新ユーザのユーザID
   * @param programName プログラム名
   */
  void markDoneByRequestIds(List<Long> requestIds, Long userId, String programName);

  /**
   * 指定されたrequest_idのステータスをFAILEDに更新する。
   *
   * @param requestIds request_idリスト
   * @param errorMessage エラーメッセージ
   * @param userId 更新ユーザのユーザID
   * @param programName プログラム名
   */
  void markFailedByRequestIds(
      List<Long> requestIds, String errorMessage, Long userId, String programName);
}
