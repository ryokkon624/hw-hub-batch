package com.hwhub.batch.application.service

import com.hwhub.batch.domain.enums.NotificationType
import com.hwhub.batch.domain.enums.ProgramType
import com.hwhub.batch.domain.model.HouseholdMemberModel
import com.hwhub.batch.domain.model.HouseholdModel
import com.hwhub.batch.domain.model.UserModel
import com.hwhub.batch.domain.model.notification.NotificationModel
import com.hwhub.batch.domain.repository.HouseholdMemeberRepository
import com.hwhub.batch.domain.repository.HouseholdMemeberRepository.HouseholdUserPair
import com.hwhub.batch.domain.repository.HouseholdRepository
import com.hwhub.batch.domain.repository.NotificationEventRepository
import com.hwhub.batch.domain.repository.NotificationRepository
import com.hwhub.batch.domain.repository.UserRepository
import spock.lang.Specification

import java.time.LocalDateTime

class NotificationAggregationServiceSpec extends Specification {

    NotificationRepository repository = Mock()
    NotificationEventRepository eventRepository = Mock()
    HouseholdRepository householdRepository = Mock()
    HouseholdMemeberRepository householdMemeberRepository = Mock()
    UserRepository userRepository = Mock()

    NotificationAggregationService service = new NotificationAggregationService(
            repository,
            eventRepository,
            householdRepository,
            householdMemeberRepository,
            userRepository
    )

    def "aggregateTaskAssignmentNotificationsは未処理イベントがない場合0件を返却すること"() {
        when: "処理対象がない場合"
        1 * eventRepository.markPendingAsProcessing(_, 2000, 2L, ProgramType.BTC_NTF_AGGR.code) >> 0
        NotificationAggregationService.Result result = service.aggregateTaskAssignmentNotifications()

        then: "0が返却されること"
        result.claimedCount() == 0
        result.notificationsInserted() == 0
        result.eventsDone() == 0
        0 * eventRepository.aggregateLatestTaskAssignmentEvents(_)
    }

    def "aggregateTaskAssignmentNotificationsはすべての名前解決の分岐を通してイベントを処理できること"() {
        given: "10件のイベントがクレイムされたとする"
        1 * eventRepository.markPendingAsProcessing(_, 2000, 2L, ProgramType.BTC_NTF_AGGR.code) >> 10

        and: "イベントが集約されて3件返る"
        LocalDateTime now = LocalDateTime.now()
        NotificationModel model1 = NotificationModel.reconstructFromEvent(1L, "0301", 100L, 200L, now, "2023/01/01", 2)
        NotificationModel model2 = NotificationModel.reconstructFromEvent(2L, "0302", 101L, 201L, now, "2023/01/02", 1)
        NotificationModel model3 = NotificationModel.reconstructFromEvent(3L, "0303", 102L, 202L, now, "2023/01/03", 5)
        // NotificationType.fromCode()がIllegalArgumentExceptionを投げるため、既存のコードを入れてdefault分岐を通す
        NotificationModel model4_real = NotificationModel.reconstructFromEvent(4L, "0101", 103L, 203L, now, "2023/01/04", 1)
        
        List<NotificationModel> rows = [model1, model2, model3, model4_real]

        1 * eventRepository.aggregateLatestTaskAssignmentEvents(_) >> rows

        and: "おうち情報をモック: 1L, 2Lは存在するが3L, 4Lはおうちが削除されているケース（または取得できない）"
        HouseholdModel h1 = HouseholdModel.reconstruct(1L, "Household 1", 10L)
        HouseholdModel h2 = HouseholdModel.reconstruct(2L, "Household 2", 10L)
        1 * householdRepository.findByIds(_) >> [h1, h2]

        and: "おうちメンバー情報（ニックネームあり／なし）をモック"
        // 1L-100Lはニックネームあり、2L-101Lはニックネームなし
        HouseholdMemberModel hm1 = HouseholdMemberModel.reconstruct(1L, 100L, null, null, null, "Nick 1", "ACTIVE", null)
        HouseholdMemberModel hm2 = HouseholdMemberModel.reconstruct(2L, 101L, null, null, null, null, "ACTIVE", null)
        1 * householdMemeberRepository.findByHouseholdAndUserPairs(_) >> [hm1, hm2]

        and: "ユーザー情報をモック（ニックネームがなかった/取得できなかった場合）"
        // 2L-101Lはニックネームなしなのでユーザーから取得。3L-102L, 4L-103Lはメンバーに存在しないケースなのでこれもユーザーから取得
        // 101Lは表示名あり、102Lは表示名なしや未設定、103Lはユーザー自体が存在しない（退会等）
        UserModel u101 = UserModel.reconstruct(101L, "e", "h", now, "l", "i", "Display 101", "ja", "k", now, true)
        UserModel u102 = UserModel.reconstruct(102L, "e2", "h", now, "l", "i", null, "ja", "k", now, true)
        // 103Lは返さない
        1 * userRepository.findByIds(_) >> [u101, u102]

        and: "データの挿入と処理完了更新が呼ばれること"
        1 * repository.bulkInsert(rows, 2L, ProgramType.BTC_NTF_AGGR.code) >> rows.size()
        1 * eventRepository.markProcessingAsDone(_, 2L, ProgramType.BTC_NTF_AGGR.code) >> rows.size()

        when: "処理を実行"
        NotificationAggregationService.Result result = service.aggregateTaskAssignmentNotifications()

        then: "期待通りの結果が返却されること"
        result.claimedCount() == 10
        result.notificationsInserted() == 4
        result.eventsDone() == 4

        and: "モデルに正しいパラメータが設定されていること"
        // model1 (Nickname)
        model1.getMessage().titleKey() == "notifications.messages.taskAssigned.title"
        model1.getMessage().bodyKey() == "notifications.messages.taskAssigned.body"
        model1.getMessage().params().get("actorName") == "Nick 1"
        model1.getMessage().params().get("household") == "Household 1"

        // model2 (DisplayName fallback)
        model2.getMessage().titleKey() == "notifications.messages.beDumpedTasks.title"
        model2.getMessage().bodyKey() == "notifications.messages.beDumpedTasks.body"
        model2.getMessage().params().get("actorName") == "Display 101"
        model2.getMessage().params().get("household") == "Household 2"

        // model3 (User ID fallback)
        model3.getMessage().titleKey() == "notifications.messages.yourTaskWasTaken.title"
        model3.getMessage().bodyKey() == "notifications.messages.yourTaskWasTaken.body"
        model3.getMessage().params().get("actorName") == "102"
        model3.getMessage().params().get("household") == null

        // model4_real (User completely not found, ID fallback & default title)
        model4_real.getMessage().titleKey() == "notifications.messages.generic.title"
        model4_real.getMessage().bodyKey() == "notifications.messages.generic.body"
        model4_real.getMessage().params().get("actorName") == "103"
        model4_real.getMessage().params().get("household") == null
    }

    def "buildHouseholdNameMapは入力が空の場合、空のマップを返却すること"() {
        given: "空のリスト"
        List<NotificationModel> rows = []
        
        when: "プライベートメソッドを通して空かテスト(Serviceの処理を通すのが難しいので、リフレクションでメソッドを呼び出すことも可能だが、Serviceメソッド経由で検証)"
        1 * eventRepository.markPendingAsProcessing(_, _, _, _) >> 1
        1 * eventRepository.aggregateLatestTaskAssignmentEvents(_) >> rows
        1 * eventRepository.markProcessingAsDone(_, _, _) >> 0

        NotificationAggregationService.Result result = service.aggregateTaskAssignmentNotifications()

        then: "0件追加で処理される"
        result.notificationsInserted() == 0
        0 * repository.bulkInsert(_, _, _)
    }

}
