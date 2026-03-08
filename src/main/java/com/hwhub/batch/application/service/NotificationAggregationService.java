package com.hwhub.batch.application.service;

import com.hwhub.batch.domain.enums.NotificationLinkType;
import com.hwhub.batch.domain.enums.NotificationType;
import com.hwhub.batch.domain.enums.ProgramType;
import com.hwhub.batch.domain.model.HouseholdMemberModel;
import com.hwhub.batch.domain.model.HouseholdModel;
import com.hwhub.batch.domain.model.UserModel;
import com.hwhub.batch.domain.model.notification.NotificationLink;
import com.hwhub.batch.domain.model.notification.NotificationMessage;
import com.hwhub.batch.domain.model.notification.NotificationModel;
import com.hwhub.batch.domain.repository.HouseholdMemeberRepository;
import com.hwhub.batch.domain.repository.HouseholdMemeberRepository.HouseholdUserPair;
import com.hwhub.batch.domain.repository.HouseholdRepository;
import com.hwhub.batch.domain.repository.NotificationEventRepository;
import com.hwhub.batch.domain.repository.NotificationRepository;
import com.hwhub.batch.domain.repository.UserRepository;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationAggregationService {

  private static final long SYSTEM_USER_ID = 2;
  // 1回のバッチで処理する最大件数
  private static final int CLAIM_LIMIT = 2000;

  private final NotificationRepository repository;
  private final NotificationEventRepository eventRepository;
  private final HouseholdRepository householdRepository;
  private final HouseholdMemeberRepository householdMemeberRepository;
  private final UserRepository userRepository;

  public record Result(int claimedCount, int notificationsInserted, int eventsDone) {}

  public record HouseholdUserKey(Long householdId, Long userId) {}

  /**
   * タスク割当系通知の集約
   *
   * <p>流れ：
   *
   * <ol>
   *   <li>PENDING -> PROCESSING (processing_key付与)
   *   <li>task_id単位で最新だけ採用 → target/type/date/actorで集約結果を取得
   *   <li>Java側で通知文言/params/link を構築して t_notification に bulkInsert
   *   <li>PROCESSING -> DONE
   * </ol>
   */
  @Transactional
  public Result aggregateTaskAssignmentNotifications() {

    String processingKey = UUID.randomUUID().toString();

    int claimed =
        eventRepository.markPendingAsProcessing(
            processingKey, CLAIM_LIMIT, SYSTEM_USER_ID, ProgramType.BTC_NTF_AGGR.getCode());

    if (claimed == 0) {
      return new Result(0, 0, 0);
    }

    // イベントを集約して取得
    List<NotificationModel> rows =
        eventRepository.aggregateLatestTaskAssignmentEvents(processingKey);

    int inserted = 0;
    if (!rows.isEmpty()) {
      // 名称を取得
      Map<HouseholdUserKey, String> actorNameMap = buildActorNameMap(rows);
      Map<Long, String> householdNameMap = buildHouseholdNameMap(rows);

      // メッセージと遷移先を設定
      rows.forEach(m -> setMessageAndLink(m, actorNameMap, householdNameMap));

      inserted = repository.bulkInsert(rows, SYSTEM_USER_ID, ProgramType.BTC_NTF_AGGR.getCode());
    }

    int done =
        eventRepository.markProcessingAsDone(
            processingKey, SYSTEM_USER_ID, ProgramType.BTC_NTF_AGGR.getCode());

    return new Result(claimed, inserted, done);
  }

  /**
   * 指定された通知モデルに遷移先とメッセージを設定する。
   *
   * @param model 通知モデル
   * @param actorNameMap 操作者名マップ
   * @param householdNameMap おうち名マップ
   */
  private void setMessageAndLink(
      NotificationModel model,
      Map<HouseholdUserKey, String> actorNameMap,
      Map<Long, String> householdNameMap) {
    // 遷移先
    NotificationLink link =
        new NotificationLink(NotificationLinkType.MY_TASKS, model.getHouseholdId());

    // i18n keyは notificationTypeから決める
    String titleKey = resolveTitleKey(model.getNotificationType());
    String bodyKey = resolveBodyKey(model.getNotificationType());

    // params
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("household", householdNameMap.get(model.getHouseholdId()));
    params.put("date", model.getAggregatedKey());
    params.put("count", model.getAggregatedCount());
    params.put(
        "actorName",
        actorNameMap.get(new HouseholdUserKey(model.getHouseholdId(), model.getActorUserId())));

    NotificationMessage message = new NotificationMessage(titleKey, bodyKey, params);

    model.setMessageAndLink(message, link);
  }

  /**
   * おうちの表示名を取得する。
   *
   * @param rows 通知モデルのリスト
   * @return おうち名のマップ
   */
  private Map<Long, String> buildHouseholdNameMap(List<NotificationModel> rows) {
    List<Long> householdIds =
        rows.stream().map(NotificationModel::getHouseholdId).distinct().toList();
    if (householdIds.isEmpty()) {
      return Map.of();
    }
    return householdRepository.findByIds(householdIds).stream()
        .collect(Collectors.toMap(HouseholdModel::getHouseholdId, HouseholdModel::getName));
  }

  /**
   * 操作者の表示名を取得する。
   *
   * <ol>
   *   <li>おうちの表示名を取得する
   *   <li>おうちの表示名がない(既におうちから離脱しているケース)場合は、ユーザーの表示名を取得する
   * </ol>
   *
   * @param rows 通知モデルのリスト
   * @return 操作者の表示名のマップ
   */
  private Map<HouseholdUserKey, String> buildActorNameMap(List<NotificationModel> rows) {

    // おうちとユーザーのペアを生成
    List<HouseholdUserPair> pairs =
        rows.stream()
            .map(r -> new HouseholdUserPair(r.getHouseholdId(), r.getActorUserId()))
            .distinct()
            .toList();
    if (pairs.isEmpty()) {
      return Map.of();
    }

    Map<HouseholdUserKey, String> nameMap = new HashMap<>();

    // おうちごとに設定している表示名を取得
    List<HouseholdMemberModel> members =
        householdMemeberRepository.findByHouseholdAndUserPairs(pairs);
    for (HouseholdMemberModel m : members) {
      if (m.getNickname() != null && !m.getNickname().isBlank()) {
        nameMap.put(new HouseholdUserKey(m.getHouseholdId(), m.getUserId()), m.getNickname());
      }
    }

    // おうちごとに設定している表示名が未設定(乃至は離脱している)のユーザのペアを生成
    List<HouseholdUserPair> missingPairs =
        pairs.stream()
            .filter(p -> !nameMap.containsKey(new HouseholdUserKey(p.householdId(), p.userId())))
            .toList();
    if (missingPairs.isEmpty()) {
      return nameMap;
    }

    // 世帯IDを無視してユーザIDでまとめる
    List<Long> missingUserIds =
        missingPairs.stream().map(HouseholdUserPair::userId).distinct().toList();

    // ユーザ情報を取得しユーザID、表示名のMap生成
    Map<Long, String> userNameMap =
        userRepository.findByIds(missingUserIds).stream()
            .filter(u -> u.getDisplayName() != null && !u.getDisplayName().isBlank())
            .collect(
                Collectors.toMap(UserModel::getUserId, UserModel::getDisplayName, (a, b) -> a));
    for (HouseholdUserPair p : missingPairs) {
      String name = userNameMap.get(p.userId());
      if (name != null) {
        nameMap.put(new HouseholdUserKey(p.householdId(), p.userId()), name);
      } else {
        // 最後のfallback(あり得ない)
        nameMap.put(new HouseholdUserKey(p.householdId(), p.userId()), String.valueOf(p.userId()));
      }
    }

    return nameMap;
  }

  /**
   * 通知種別によってメッセージtitleを設定する。
   *
   * @param type 通知種別
   * @return メッセージtitle
   */
  private String resolveTitleKey(NotificationType type) {
    return switch (type) {
      case TASK_ASSIGNED -> "notifications.messages.taskAssigned.title";
      case BE_DUMPED_TASK -> "notifications.messages.beDumpedTasks.title";
      case YOUR_TASK_WAS_TAKEN -> "notifications.messages.yourTaskWasTaken.title";
      default -> "notifications.messages.generic.title";
    };
  }

  /**
   * 通知種別によってメッセージbodyを設定する。
   *
   * @param type 通知種別
   * @return メッセージbody
   */
  private String resolveBodyKey(NotificationType type) {
    return switch (type) {
      case TASK_ASSIGNED -> "notifications.messages.taskAssigned.body";
      case BE_DUMPED_TASK -> "notifications.messages.beDumpedTasks.body";
      case YOUR_TASK_WAS_TAKEN -> "notifications.messages.yourTaskWasTaken.body";
      default -> "notifications.messages.generic.body";
    };
  }
}
