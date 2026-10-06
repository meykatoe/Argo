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
- 額外折扣與後台 API：每張卡有 `extra_discount`（預設 1，範圍 0 到 1），最終售價為「市價 × 倍率 × 額外折扣」，手動改價的卡片不套用。前台在有折扣時以刪除線顯示折前價，並在折後價旁標示紅色 `(SALE!!)`。
- 後台人員帳號：不開放註冊，帳號只能由人工以指令建立。密碼以 BCrypt 雜湊存放（`staff_account`）；登入後發給隨機令牌，資料庫只存令牌雜湊（`staff_session`），預設 8 小時過期，登出即失效，停用帳號立即生效。連續輸入錯誤密碼 5 次會鎖定 15 分鐘（`argo.admin.max-failures`、`argo.admin.lock-minutes`）。角色分為 `ADMIN`（最高權限）、`GENERAL`（一般管理員）、`SERVICE`（客服）、`OPS`（運維，只能使用運維後台），三者之間沒有隱含的高低繼承；各角色能使用什麼功能不寫在程式裡，而是存在資料庫的選單權限表（見下方「後台選單與權限」）。建立帳號：
  ```
  cd backend
  STAFF_PASSWORD='至少十個字元的密碼' ./mvnw spring-boot:run -Dspring-boot.run.arguments="--argo.staff.create=alice --argo.staff.role=general"
  ```
  `--argo.staff.role` 必填（`admin` / `general` / `service` / `ops`）。建立完成後程式會自動結束；未設定 `STAFF_PASSWORD` 時會在終端機提示輸入。帳號名稱為 3 到 50 字元的小寫英數與 `. _ -`。停用帳號：`update staff_account set enabled = false where username = 'alice';`
  - `POST /api/admin/auth/login`：`{"username": ..., "password": ...}`，回傳 `token`、`role`、`expiresAt`；帳號或密碼錯誤一律回 `LOGIN_FAILED`，鎖定中回 429 `LOGIN_LOCKED`。其餘後台 API 需帶標頭 `Authorization: Bearer <token>`。
  - `POST /api/admin/auth/logout`、`GET /api/admin/auth/me`。
  - `GET /api/admin/cards`（`ADMIN`、`GENERAL`）：卡片列表（`keyword`、`setId`、`discounted`、`page`、`size`），回傳折前價、額外折扣與售價。
  - `PATCH /api/admin/cards/{id}/extra-discount`（`ADMIN`、`GENERAL`）：body 為 `{"extraDiscount": 0.4}`，最多四位小數，改完即時重算售價。
- 後台操作稽核（`staff_audit_log`）：記錄誰（帳號與角色的當下快照）、在何時（`created_at`）、從哪裡（IP、User-Agent）、做了什麼（`action`）、對象是誰（`target_type`、`target_id`）、是否成功，以及變更前後內容（`detail`，JSON）。目前會記錄：登入成功、登入失敗（含帳號不存在、密碼錯誤、帳號停用，失敗原因寫在 `detail`，不記錄密碼）、登入鎖定、登出、越權存取被拒、以指令建立帳號（操作者記為 `cli:系統使用者`）、修改額外折扣（含修改前後的折扣與售價）。修改類操作與稽核紀錄在同一個資料庫交易中寫入，不會出現「改了卻沒紀錄」。資料表以觸發器禁止 `UPDATE` 與 `DELETE`，帳號因此也不能刪除，請改用停用。IP 取自連線位址；若之後放在反向代理後面，需另外設定轉送標頭，否則記到的會是代理的位址。新增後台功能時，請在寫入操作中呼叫 `AuditLogService.record(...)`。
- 運維後台 API（`/api/ops/**`）：與業務後台（`/api/admin/**`）分開，方便日後在反向代理或網關層限制只有內網或指定 IP 能連線。兩個入口各有登入端點（`/api/admin/auth/login`、`/api/ops/auth/login`），帳號只能從自己的入口登入：`OPS` 帳號不能登入業務後台，其他角色不能登入運維後台，走錯入口一律回 `LOGIN_FAILED`，並在稽核紀錄寫入 `WRONG_PORTAL`。`OPS` 的令牌也不能呼叫業務後台端點。
  - `GET /api/ops/audit-logs`（只有 `OPS`，`ADMIN` 也不可看）：查詢稽核紀錄，依時間新到舊。參數：`username`（不分大小寫、包含比對）、`action`、`success`、`targetType`、`targetId`、`from`、`to`（ISO 8601 時間，含起不含迄）、`page`、`size`（上限 100，預設 50）。每次查詢本身也會寫入稽核紀錄（`AUDIT_LOG_VIEWED`，記下篩選條件）。
- 後台選單與權限（資料庫驅動）：`admin_menu` 是選單樹，同時也是功能權限的節點（`portal` 區分業務後台 `ADMIN` 或運維後台 `OPS`；`parent_id` 組成「群組 → 子選項」；`code` 是權限代碼；`path` 是前端頁面路徑，群組為空；`sort_order` 數字小的在前；`enabled` 可整個停用）。`role_menu` 記錄哪個角色可使用哪個節點。後端端點只寫權限代碼，例如 `@RequirePermission("card.edit")`；只需登入的端點（登出、取得選單）用 `@AnyStaff`；沒有標註的端點一律拒絕。權限判斷每次都查資料庫，所以調整後立即生效。`GET /api/admin/menu`、`GET /api/ops/menu` 回傳目前登入者可見的選單樹：葉節點需被授權才顯示，群組至少有一個可見子選項才顯示。目前的配置：`card`（卡牌管理）下有 `card.edit`（卡牌編輯，`/cards`）授權給 `ADMIN`、`GENERAL`；`audit`（稽核管理）下有 `audit.logs`（稽核紀錄，`/audit-logs`）授權給 `OPS`。新增選項與授權範例（不需改程式，但該選項對應的前端頁面與後端端點要已存在）：
  ```
  insert into admin_menu (parent_id, portal, code, title, path, sort_order)
  values ((select id from admin_menu where code = 'card'), 'ADMIN', 'card.series', '卡牌系列', '/series', 20);
  insert into role_menu (role, menu_id)
  select 'GENERAL', id from admin_menu where code = 'card.series';
  ```
  收回授權：`delete from role_menu where role = 'GENERAL' and menu_id = (select id from admin_menu where code = 'card.series');`
- 後台（`frontend/admin`）：工作人員以帳號密碼登入（登入資料只存在該分頁的 `sessionStorage`，關閉分頁即登出，過期自動失效），右上角顯示帳號與角色。`ADMIN`、`GENERAL` 可依卡號或卡名搜尋、只看有折扣的卡，直接修改每張卡的額外折扣（輸入 0.4 會顯示為 4 折），儲存後立即顯示新售價，手動定價的卡片不可設定折扣；`SERVICE` 目前登入後顯示「目前尚無可用功能」。帳號需先以上述指令建立。啟動方式：後端啟動後，於 `frontend/admin` 執行 `npm install && npm run dev`（連接埠 5174）。正式部署時需把後台網址加入 `argo.cors.origins`，或與後端放在同一網域下反向代理。
- 運維後台（`frontend/ops`）：`OPS` 帳號以帳號密碼登入（走 `/api/ops/auth/login`），目前提供稽核紀錄頁：依帳號、動作、成功或失敗、時間範圍篩選，每頁 50 筆，新的在前；失敗的紀錄會標紅，點「詳情」可看對象、User-Agent 與完整的變更前後 JSON，折扣修改會直接摘要為「折扣 1 → 0.4，售價 9.00 → 3.60」。啟動方式：於 `frontend/ops` 執行 `npm install && npm run dev`（連接埠 5175）。正式部署時建議只在內網提供此網站與 `/api/ops/**`。
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
    ├── admin/          業務後台前端（Vue，ADMIN、GENERAL、SERVICE 使用）
    └── ops/            運維後台前端（Vue，只有 OPS 使用）
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
