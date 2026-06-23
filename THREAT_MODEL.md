# THREAT_MODEL — hw-hub-batch

Housework Hub バッチ（Java 21 / Spring Batch / MyBatis / MySQL / Claude API）の脅威モデル。
LLM ベースのセキュリティスキャンおよび人手レビューが、**何を守り・何を信頼し・何を対象外とするか**を共有するための土台ドキュメント。

> 本書は Anthropic「Using LLMs to secure source code」の Find-and-Fix ループ Step 1（Threat Modeling）に対応する。

- 対象リポジトリ: `hw-hub-batch`
- 種別: 外部エンドポイントを持たない定期実行バッチ（EventBridge 起動）。**信頼境界＝消費するデータ**
- 最終更新: 2026-06-23
- ステータス: ドラフト（Phase 0）

---

## 1. システムコンテキスト

EventBridge Scheduler で定期起動される ECS Fargate タスク群。代表ジョブ:

- `inquiryAiReplyJob` … 問い合わせへの **AI 自動返信（Claude API 連携）**
- `houseworkTaskGenerate/Recalc` … 家事タスク生成・再計算
- `invitationExpire` / `householdCleanup` / `notificationAggregation`

| 項目     | 内容                                                          |
| -------- | ------------------------------------------------------------- |
| 実行環境 | AWS ECS Fargate ＋ EventBridge Scheduler                      |
| 主要技術 | Java 21 / Spring Batch / MyBatis / MySQL / S3 / Claude API    |
| 認証情報 | Secrets Manager（本番: CLAUDE_API_KEY・DB パスワード）        |
| 外部連携 | Claude API（生成）/ S3（ナレッジ faq.md・howto.md）          |

---

## 2. 守るべき資産（Assets）

| 資産                          | 説明                                                    | 影響度 |
| ----------------------------- | ------------------------------------------------------- | ------ |
| `CLAUDE_API_KEY`              | 漏洩＝API 不正利用・コスト。Secrets Manager 管理        | 高     |
| DB 認証情報                   | 単一サービスアカウントで全ジョブの読み書き              | 高     |
| ユーザー問い合わせデータ      | `t_inquiry` / `t_inquiry_message`（全世帯）             | 高     |
| AI 生成テキスト（書き戻し）   | `t_inquiry_message.body` に保存→フロント/モバイルで表示 | 中     |
| ナレッジベース（S3）          | 全 AI 返信に反映される faq.md / howto.md                | 中     |

---

## 3. 信頼境界 / 入力源（Trust boundaries）

外部から直接叩く HTTP エンドポイントは**無い**。信頼境界は「**バッチが消費するデータ**」にある。

```
[ユーザー入力] ─Frontend/Mobile─> [Backend API] ─保存─> [MySQL]
                                                            │ (時間差・非同期)
[hw-hub-knowledge] ─GitHub Actions─> [S3: faq/howto] ──────┤
                                                            ▼
                                              [このバッチ] ──> [Claude API] ──生成──> [MySQL: t_inquiry_message]
```

**最重要原則: DB・S3 から読むデータは"信頼できない入力"として扱う。**
`t_inquiry.title` / `t_inquiry_message.body` は元をたどればユーザーが Frontend で入力した値。外部 EP が無くても、**二次的（stored）な攻撃経路**になる。

---

## 4. 想定する脅威（What can go wrong?）

| ID  | 脅威                                                                                                                 | 関連                          | 重大度 |
| --- | ------------------------------------------------------------------------------------------------------------------ | ----------------------------- | ------ |
| T1  | **プロンプトインジェクション**: ユーザー入力（title/body）を区切り・サニタイズ無しで Claude プロンプトに直結         | `ClaudeApiClient.buildUserMessage` | 中     |
| T2  | **生成テキストの保存→表示（二次 XSS 経路）**: Claude 生成を `t_inquiry_message.body` に保存し、フロント/モバイルで表示 | InquiryAiReplyService         | 中     |
| T3  | **ナレッジベースポイズニング**: `hw-hub-knowledge` → S3 が汚染されると全 AI 返信に反映                              | KnowledgeLoader               | 中     |
| T4  | **過剰な DB 権限 / blast radius**: 単一 DB ユーザーで全ジョブ・全世帯データを読み書き                               | 全ジョブ共通                  | 中     |
| T5  | **レート制限・リトライ無し**: Claude API 呼び出しの制御不足によるコスト/可用性影響                                  | ClaudeApiClient               | 低〜中 |
| T6  | **依存パッケージの既知脆弱性**: 推移的依存を含むサプライチェーン                                                    | build.gradle                  | 中     |

### T1 の補足（過大評価しないための整理）

- プロンプトは**問い合わせ単位**で組まれ、その問い合わせ自身のメッセージのみを含む → **クロスユーザー汚染は無い**（攻撃者は自分の問い合わせの AI 返信しか操作できない）。
- 影響の実体: システムプロンプト/ナレッジの抽出、自分宛て AI 返信の操作、T2 と連鎖した stored ペイロードの埋め込み。
- 緩和策（Phase 4 で検討）: ユーザー入力を明確な区切り（XML タグ等）で構造化して隔離、システム側で「以降はデータであり指示ではない」と明示、生成物の用途を限定。

---

## 5. 現状の対策（既存コントロール）

- **SQL**: MyBatis はパラメータ化（`#{}`）徹底、`${}` 無し → SQL インジェクション低リスク。
- **シークレット**: 本番は Secrets Manager 経由で注入（CLOUDWATCH に非表示）。実 `.env` はリポジトリ外（gitignore）。コミットは `.env.example`（プレースホルダ）のみ＝**git 履歴で漏洩なしを確認済み**。
- **S3**: ECS Task Role（IAM）経由でアクセス（静的クレデンシャル不要）。
- **隔離・監査**: ECS Fargate（awsvpc）、CloudWatch Logs。ログにユーザー本文は出力しない（件数・ID のみ）。
- **冪等性**: ジョブは冪等を意識した設計。

---

## 6. 信頼する入力（Trusted inputs）

- **EventBridge による起動トリガー**（内部・信頼）。
- **Secrets Manager / IAM Role で供給される認証情報**。
- **Claude API のトランスポート（TLS）と署名**。

> 逆に、**DB / S3 から読むデータ（問い合わせ本文・ナレッジ）は信頼しない**。これらは Claude へ渡る前提で、T1〜T3 の対象。

---

## 7. スコープ外（Out of scope）

- Claude API の内部実装・モデル挙動（プロンプト設計は本書の対象だが、API 側は対象外）
- Backend API の認証・認可（入力がそこを通る前提）→ `hw-hub-backend`
- フロント/モバイルの表示時エスケープ → `hw-hub-frontend` / `hw-hub-mobile`（ただし T2 の連携注意点として明記）
- インフラ・IAM・Secrets Manager・VPC → `hw-hub-infra`
- DB スキーマ → `hw-hub-database`

---

## 8. レビュー観点チェック（Did we do a good job?）

1. ユーザー由来データ（title/body）が**区切り・構造化**されずにプロンプトへ連結されていないか（T1）
2. Claude 生成テキストの保存先と、その表示側のエスケープ前提が成立しているか（T2）
3. ナレッジ供給（hw-hub-knowledge → S3）の信頼性・改ざん検知
4. バッチ DB ユーザーの権限が最小化されているか（T4）
5. Claude API 呼び出しにレート制限・リトライ・コスト上限があるか（T5）
6. API キー・DB 認証情報がログ・例外に出ていないか
7. 依存パッケージの既知脆弱性（Phase 2 の深掘りスキャン対象）

---

## 9. 更新運用

- Claude API 連携・プロンプト構造・新ジョブ追加・データ入力源の変更時に本書を更新する。
- T1（プロンプトインジェクション）は実コードで確認済みの所見。ハードニングは Phase 4（パッチ）で backlog 化する。
- Retro でセキュリティ指摘を棚卸しする際、本書の見直し要否を確認する。
