Argo: 傳説中尋找金羊毛的船，象徵尋寶之旅

## 爲什麽會誕生這個專案

從兒時就很喜歡TCG卡牌游戲，有人喜歡用其對戰社交，也有人單純喜歡收藏。而無論哪一種我都認爲，卡片是珍貴的，承載著很多的回憶，因此將專案命名為尋寶的船，就是希望用戶帶著找到自己珍愛卡片的心，去使用網站。

## 使用語言及技術框架

- 後端：Java 25、Spring Boot 4.1、Maven、Spring Data JPA、Flyway
- 資料庫：PostgreSQL 18
- 前端：Vue 3、Vite、TypeScript（每個前端獨立一個資料夾）
- 卡片資料來源：optcgapi.com

## 功能概述（實時更新）

- 卡片資料同步：從 optcgapi.com 取得補充包、起始牌組、促銷卡，寫入資料庫。每天凌晨 4 點自動同步，也可用 `--argo.sync.on-startup=true` 在啟動時手動同步一次。
- 卡片查詢 API（無需登入，皆為 GET）：
  - `/api/cards`：列表，支援 `keyword`、`setId`、`category`（booster / starter / promo）、`color`、`rarity`、`cardType` 篩選，`page`（從 1 開始）、`size`（上限 100）、`sortBy`（`cardSetId` / `cardName` / `marketPrice`）、`desc` 分頁排序
  - `/api/cards/{id}`：卡片詳情
  - `/api/sets`：系列列表，可用 `category` 篩選

## 專案架構

```
Argo/
├── backend/            後端 API（Spring Boot）
└── frontend/
    └── official-site/  官網前端（Vue）
```

前端依用途命名放在 `frontend/` 下，未來新增前端時各自獨立一個資料夾。

## 命名規範

- Java 程式（類別、欄位、方法）及 API 的 JSON 欄位使用駝峰命名（camelCase）。
- 資料庫的資料表與欄位使用底線命名（snake_case）。
- 兩者靠 Spring 預設命名策略自動對應，例如 `cardSetId` 對應 `card_set_id`，實體類別不需手寫欄位名稱。

## 本機開發

資料庫預設連線：`127.0.0.1:5432`，資料庫 `argo`，帳號 `argo`。
可用環境變數 `DB_URL`、`DB_USER`、`DB_PASSWORD` 覆蓋。

```
cd backend && ./mvnw spring-boot:run
```

## 未來會想做的功能（實時更新）
