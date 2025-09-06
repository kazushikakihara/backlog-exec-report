# Backlog Executive Report (Spring Boot)

Backlog の API と Spring Boot を使い、中間管理職が役員報告に使える **HTML/PDF レポート**を生成します。

## 必要な環境変数
- `BACKLOG_SPACE_HOST` 例: `yourspace.backlog.com`
- `BACKLOG_API_KEY`    Backlog 個人 API キー
- `CLOSED_STATUS_NAMES` クローズ扱いステータス名（カンマ区切り, 例: `Closed,Resolved`）

## 起動（Codespaces など）
```bash
# ビルド
mvn -q -DskipTests package
# 実行
mvn spring-boot:run
```

## エンドポイント
- HTML: `/reports/executive?since=YYYY-MM-DD&until=YYYY-MM-DD&projectKeys=PRJ1,PRJ2`
- PDF : `/reports/executive.pdf?since=YYYY-MM-DD&until=YYYY-MM-DD&projectKeys=...`