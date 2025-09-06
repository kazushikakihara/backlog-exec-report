# Backlog Executive Report (Spring Boot)

Backlog の API と Spring Boot を使い、中間管理職が役員報告に使える **HTML/PDF レポート**を生成します。

## 主な機能

* プロジェクトごとの **Open / Closed / 期間内新規 / 期限超過(Overdue)** を一覧表示
* **PDF ダウンロード**（OpenHTMLtoPDF）
* **アーカイブ済みプロジェクトも対象**（全件取得）
* Backlog の **プロジェクト別ステータス** API を利用

## 必要な環境変数

* `BACKLOG_SPACE_HOST` 例: `yourspace.backlog.com`（`.jp` の場合は `yourspace.backlog.jp`）
* `BACKLOG_API_KEY`    Backlog 個人 API キー
* `CLOSED_STATUS_NAMES` クローズ扱いステータス名（カンマ区切り, 例: `Closed,Resolved,完了,対応済み`）

> セキュリティ: APIキーは**コミットしない**でください。Codespaces を使う場合は **Repository secrets** に保存するのが推奨です。

## セットアップ（Backlog 側）

1. 自分のスペースにログイン → **個人設定 → API** → **新しいAPIキーを発行**
2. 2要素認証(MFA)が必須のスペースでは、**ユーザー側のMFA有効化が必要**です。

## セットアップ（Codespaces 推奨）

1. GitHub リポジトリ → **Settings → Security → Secrets and variables → Codespaces**
2. **New repository secret** で以下を登録

   * `BACKLOG_SPACE_HOST`：`yourspace.backlog.com`（または `.jp`）
   * `BACKLOG_API_KEY`：発行した個人APIキー
   * 任意 `CLOSED_STATUS_NAMES`：`Closed,Resolved,完了,対応済み` など
3. Codespace を開いて **F1 → “Codespaces: Rebuild Container”** で反映

### 反映確認

```bash
echo $BACKLOG_SPACE_HOST
echo ${BACKLOG_API_KEY:0:4}****
curl -sS "https://${BACKLOG_SPACE_HOST}/api/v2/users/myself?apiKey=${BACKLOG_API_KEY}" -w "\nHTTP %{http_code}\n"
# → HTTP 200 と自分のユーザー情報が返ればOK
```

## 起動（Codespaces / ローカル）

### Maven Wrapper を使用（推奨）

```bash
# ビルド
./mvnw -q -DskipTests package
# 実行
./mvnw spring-boot:run
```

> ローカルに Maven がある場合は `mvn` でも可。

### ポート公開（Codespaces）

* Ports タブで **8080 → Open in Browser**
* または:

```bash
export BASE="https://${CODESPACE_NAME}-8080.app.github.dev"
echo $BASE
```

## エンドポイント

* HTML:
  `/reports/executive?since=YYYY-MM-DD&until=YYYY-MM-DD&projectKeys=PRJ1,PRJ2`
  ※`projectKeys` を省略すると**全プロジェクト**（アーカイブ含む）が対象
* PDF :
  `/reports/executive.pdf?since=YYYY-MM-DD&until=YYYY-MM-DD&projectKeys=...`

### 例

```
$BASE/reports/executive?since=2025-08-01&until=2025-09-05
$BASE/reports/executive.pdf?since=2025-08-01&until=2025-09-05
```

## ヒント

* **プロジェクトキーの確認**

  ```bash
  curl -sS "https://${BACKLOG_SPACE_HOST}/api/v2/projects?apiKey=${BACKLOG_API_KEY}" \
  | python3 - <<'PY'
  import sys, json
  for p in json.load(sys.stdin):
      print(f"{p['projectKey']}\t{p['name']}")
  PY
  ```
* **ステータス名の確認（クローズ扱いの調整に）**

  ```bash
  PK=<PROJECT_KEY>
  curl -sS "https://${BACKLOG_SPACE_HOST}/api/v2/projects/${PK}/statuses?apiKey=${BACKLOG_API_KEY}" \
  | python3 - <<'PY'
  import sys, json
  for s in json.load(sys.stdin):
      print(s["id"], s["name"])
  PY
  ```
* **クローズ扱いの調整**（例：日本語運用）

  ```bash
  export CLOSED_STATUS_NAMES='Closed,Resolved,完了,対応済み'
  ```

## 実装メモ

* **件数集計**は `/api/v2/issues/count` を利用（転送量・レートに優しい）

  * クエリ: `projectId[]`, `statusId[]`, `createdSince/Until`, `updatedSince/Until`, `dueDateUntil` など
  * 仕様差異のため **`hasDueDate` は /issues/count には送信しません**（400 を回避）
* **ステータス**は **プロジェクトごと**に `/projects/:projectKey/statuses` から取得
* **Overdue** は「今日までに `dueDate` が過去」「かつクローズ扱い外のステータス」でカウント
* **アーカイブ含む全件**: `getProjects(null)` を使用
* **PDF**: OpenHTMLtoPDF（`openhtmltopdf-core` / `-pdfbox`）

## トラブルシュート

* `HTTP 401 RequiredMFAError` → Backlog アカウントで **MFA を有効化**
* `HTTP 401 Invalid API key` → APIキー貼り間違い／改行混入を確認。必要なら再発行
* `HTTP 400 on /issues/count` → `hasDueDate` を送らない（本アプリは対策済み）
* 500/Whitelabel → ステータスやプロジェクトが 0 件のときに落ちないよう修正済み。最新版を再ビルド
* 8080 が開かない → アプリ未起動。`./mvnw spring-boot:run` を実行

## カスタマイズ例

* **Open件数の多い順に並び替え**

  ```java
  // ReportService#buildReport の rows 作成後
  rows.sort(java.util.Comparator.comparingInt((ProjectRow r) -> r.getOpenTotal()).reversed());
  ```
* **プロジェクト絞り込み**は `projectKeys=PRJ1,PRJ2` を付与