Argo: 傳説中尋找金羊毛的船，象徵尋寶之旅

## 爲什麽會誕生這個專案

從兒時就很喜歡TCG卡牌游戲，有人喜歡用其對戰社交，也有人單純喜歡收藏。而無論哪一種我都認爲，卡片是珍貴的，承載著很多的回憶，因此將專案命名為尋寶的船，就是希望用戶帶著找到自己珍愛卡片的心，去使用網站。

## 使用語言及技術框架

- 後端：Java 25、Spring Boot 4.1、Maven、Spring Data JPA、Flyway
- 資料庫：PostgreSQL 18
- 前端：Vue 3、Vite、TypeScript（每個前端獨立一個資料夾）
- 卡片資料來源：optcgapi.com（英文、價格）、Bandai 官方繁中卡表（繁體中文名稱與效果）

## 功能概述（實時更新）

- 卡片資料同步：從 optcgapi.com 取得補充包、起始牌組、促銷卡，寫入資料庫。每天凌晨 4 點自動同步，也可用 `--argo.sync.on-startup=true` 在啟動時手動同步一次。
- 售價與庫存：每張卡有獨立的 `sale_price`（售價，目前幣別為美元）與 `stock`（庫存）欄位。售價在同步卡片資料時以「市價 × 倍率」計算並存入資料庫（倍率為 `argo.pricing.sale-rate`，預設 0.9，改倍率後需重新同步才會生效），不是即時運算。手動改價的卡片（`price_overridden`）同步時不會被覆蓋。新卡庫存為 0；開發時可加 `--argo.dev.seed-stock=5` 啟動，替有定價且庫存為 0 的卡補上庫存。查詢 API 可用 `inStock=true` 只看可購買的卡（有庫存且已定價），`sortBy` 可用 `salePrice`。
- 額外折扣與後台 API：每張卡有 `extra_discount`（預設 1，範圍 0 到 1），最終售價為「市價 × 倍率 × 額外折扣」，手動改價的卡片不套用。前台在有折扣時以刪除線顯示折前價，並在折後價旁標示紅色 `(SALE!!)`。後台 API 以請求標頭 `X-Admin-Token` 驗證，令牌由環境變數 `ADMIN_TOKEN` 設定，未設定時後台 API 一律回 401：
  - `GET /api/admin/cards`：卡片列表（`keyword`、`setId`、`discounted`、`page`、`size`），回傳折前價、額外折扣與售價。
  - `PATCH /api/admin/cards/{id}/extra-discount`：body 為 `{"extraDiscount": 0.4}`，最多四位小數，改完即時重算售價。
- 後台（`frontend/admin`）：工作人員輸入後台令牌登入（令牌只存在該分頁的 `sessionStorage`），可依卡號或卡名搜尋、只看有折扣的卡，直接修改每張卡的額外折扣（輸入 0.4 會顯示為 4 折），儲存後立即顯示新售價。手動定價的卡片不可設定折扣。啟動方式：後端以 `ADMIN_TOKEN=自訂令牌` 啟動，再於 `frontend/admin` 執行 `npm install && npm run dev`（連接埠 5174）。正式部署時需把後台網址加入 `argo.cors.origins`，或與後端放在同一網域下反向代理。
- 多語系卡片資料：卡片查詢 API 加上 `lang` 參數（`en` 預設、`zh-TW`）即回傳該語言的卡名、效果、特徵與系列名稱，找不到翻譯時回退為英文，原文固定放在 `cardNameEn`、`setNameEn`。繁中資料抓取自 Bandai 官方繁中卡表（`asia-tc.onepiece-cardgame.com`），啟動時加 `--argo.translation.on-startup=true` 手動同步，也會每週一凌晨 5 點自動同步。內容版權屬原權利人，正式營運前請自行確認使用條款。
- 卡片查詢 API（無需登入，皆為 GET）：
  - `/api/cards`：列表，支援 `keyword`、`setId`、`category`（booster / starter / promo）、`color`、`rarity`、`cardType` 篩選，`page`（從 1 開始）、`size`（上限 100）、`sortBy`（`cardSetId` / `cardName` / `marketPrice`）、`desc` 分頁排序
  - `/api/cards/{id}`：卡片詳情
  - `/api/sets`：系列列表，可用 `category` 篩選
  - 以上三個都支援 `lang`；`/api/cards` 的 `keyword` 在 `zh-TW` 時也會比對中文卡名
- 訂單與付款（後端 API）：訪客下單，不需登入，以「訂單編號 + 下單 Email」查詢。
  - `POST /api/orders`：建立訂單。下單當下以單一語句原子扣庫存（不會超賣），明細保留當下的卡名與單價快照；未付款訂單保留 30 分鐘（`argo.order.expire-minutes`），逾時由排程自動取消並歸還庫存。
  - `GET /api/orders/{orderNo}?email=`：查詢訂單。
  - `POST /api/orders/{orderNo}/pay`：付款。目前為假信用卡，只存卡號末四碼，不存完整卡號與安全碼。測試卡：`4242 4242 4242 4242` 成功；`4000 0000 0000 0002` 拒絕、`4000 0000 0000 9995` 餘額不足、`4000 0000 0000 0119` 處理錯誤；其餘通過檢查碼的卡號皆成功；失敗可重試。付款邏輯在 `PaymentGateway` 介面後，之後對接綠界時新增實作即可。
  - `POST /api/orders/{orderNo}/cancel`：取消未付款訂單並歸還庫存。
  - 錯誤一律回傳 `{"code": ..., "status": ..., "details": {...}}`，欄位驗證失敗的 `details` 會列出有問題的欄位。
- 購物車（官網）：卡片列表與詳情可加入購物車，右上角購物車圖示顯示件數；購物車只在瀏覽器保存商品編號與數量（`localStorage`），開啟購物車頁時才向後端取得最新售價與庫存（`GET /api/cards/batch?ids=1,2,3`，一次最多 50 筆），數量超過庫存會自動調降、缺貨與下架商品不計入合計。
- 結帳與訂單（官網）：購物車「前往結帳」進入結帳頁，填寫購買人與收件資料（欄位會先在前端驗證，規則與後端一致），送出後建立訂單並清空購物車，接著到訂單頁以假信用卡付款。訂單頁顯示付款倒數（逾時自動取消）、可取消未付款訂單；訪客以「訂單編號 + Email」查詢（`/orders`），下單後的 Email 只暫存在該分頁的 `sessionStorage`，關閉分頁後需重新輸入。開發模式下付款頁會顯示測試卡號提示，正式打包後不會顯示。卡號與安全碼只存在表單元件中，付款完成或離開頁面即清除。
- 官網多語系（vue-i18n）：介面支援繁體中文（預設）與英文，右上角可切換，選擇會記在瀏覽器。介面文字放在 `src/i18n/locales/`，新增語言時加一個語言檔並登錄到 `src/i18n/index.ts`；卡片資料的翻譯則由後端依 `lang` 回傳，切換語言會自動重新取資料。
- 官網頁面：首頁（系列入口與搜尋）、卡片列表（搜尋、類別 / 系列 / 顏色 / 稀有度 / 種類 / 只看有貨篩選、依編號 / 名稱 / 價格排序、分頁，條件同步在網址，缺貨卡片有標示）、卡片詳情（售價、參考市價、庫存狀態）

## 專案架構

```
Argo/
├── backend/            後端 API（Spring Boot）
└── frontend/
    ├── official-site/  官網前端（Vue）
    └── admin/          後台前端（Vue，工作人員使用）
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
cd frontend/official-site && npm install && npm run dev
```

官網預設在 `http://localhost:5173`，開發時 `/api` 會自動轉發到後端 8080。

## 未來會想做的功能（實時更新）
