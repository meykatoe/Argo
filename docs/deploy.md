# 部署說明

本文件說明如何把後端、三個前端與資料庫跑起來。

## 架構

- 後端：Spring Boot，監聽 8080，只提供 `/api`。
- 三個前端：官網、管理後台、營運後台，各自是一包靜態檔，由 nginx 提供。
- 資料庫：PostgreSQL 18，表結構由後端啟動時自動建立。
- 反向代理：nginx 把 `/api` 轉給後端，並帶上使用者的真實 IP。

## 快速試跑

```
docker compose up --build
```

- 官網 http://localhost:5173、管理後台 5174、營運後台 5175、後端 8080。
- `curl http://localhost:8080/api/ping` 應回 200。
- 這份設定使用開發用密碼與綠界測試商店，只能試跑，不能上線。

建立員工帳號（沒有註冊功能，這是刻意的）：

```
docker compose run --rm -e STAFF_PASSWORD='Min8WithUpperLowerDigit' backend \
  java -jar /app/app.jar --argo.staff.create=NAME --argo.staff.role=ops
```

角色可選 `admin`、`general`、`service`、`ops`。

## 正式環境

1. 設定 `SPRING_PROFILES_ACTIVE=prod`。此時若設定不安全，後端會拒絕啟動（`ProdConfigGuard`）：
   - 付款模式不是 `ecpay`
   - 資料庫密碼是空的或開發預設值
   - 綠界商店、金鑰、IV、結帳網址仍是測試值
   - 官網網址、付款回呼網址、CORS 來源指向本機
2. 模擬付款端點在 prod 下不存在（404）。
3. 全程使用 HTTPS。後端偵測到 HTTPS 才會送出 HSTS。

### 一定要設對：受信任的反向代理

後端只在連線來源屬於 `ARGO_SECURITY_TRUSTED_PROXIES` 時才採用 `X-Forwarded-For`。

- 沒設或設錯：所有人會被當成同一個 IP（反向代理的 IP），限流與自動封鎖會一次影響全站。
- 請設成反向代理的 IP 或網段，多個用逗號分隔，例如 `10.0.0.5` 或 `172.28.0.0/16`。
- 反向代理必須用 `X-Forwarded-For` 帶上用戶端 IP，並且不要信任用戶端自己送來的同名標頭。範例 `deploy/web/nginx.conf.template` 用 `$remote_addr` 覆蓋它。
- 不要把後端的 8080 直接開放到公網，否則可以繞過反向代理。

### 環境變數

Spring 會把環境變數轉成設定：大寫、點與連字號改成底線，例如 `argo.ratelimit.general-per-minute` 對應 `ARGO_RATELIMIT_GENERAL_PER_MINUTE`。

必填（prod）：

| 變數 | 說明 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 設為 `prod` |
| `DB_URL` | 例如 `jdbc:postgresql://db:5432/argo` |
| `DB_USER` | 資料庫帳號 |
| `DB_PASSWORD` | 資料庫密碼，不可為 `1234` |
| `ARGO_PAYMENT_MODE` | 必須是 `ecpay` |
| `ARGO_PAYMENT_API_URL` | 綠界連得到的後端公開網址 |
| `ARGO_ECPAY_MERCHANT_ID` | 綠界商店代號 |
| `ARGO_ECPAY_HASH_KEY` | 綠界 HashKey |
| `ARGO_ECPAY_HASH_IV` | 綠界 HashIV |
| `ARGO_ECPAY_CHECKOUT_URL` | 綠界正式結帳網址 |
| `ARGO_SITE_URL` | 官網公開網址，信件連結與付款後導回用 |
| `ARGO_CORS_ORIGINS` | 三個前端的網址，逗號分隔，少一個該前端就會出現未知錯誤 |
| `ARGO_SECURITY_TRUSTED_PROXIES` | 見上節 |

選填（括號內是預設值）：

| 變數 | 說明 |
| --- | --- |
| `ARGO_MAIL_FROM` | 寄件人（`no-reply@argo.local`） |
| `SPRING_MAIL_HOST`、`SPRING_MAIL_PORT`、`SPRING_MAIL_USERNAME`、`SPRING_MAIL_PASSWORD` | 設了 host 才會真的寄信，否則只寫進日誌 |
| `ARGO_ORDER_SHIPPING_FEE` | 運費（`0.00`） |
| `ARGO_ORDER_EXPIRE_MINUTES` | 未付款保留分鐘數（`30`） |
| `ARGO_PRICING_SALE_RATE` | 售價倍率（`0.9`） |
| `ARGO_FX_FALLBACK_RATE` | 抓不到匯率時的備援匯率（`32.0`） |
| `ARGO_SYNC_ON_STARTUP`、`ARGO_SYNC_SCHEDULED`、`ARGO_SYNC_CRON` | 卡片同步（啟動不同步、排程開、每天 04:00） |
| `ARGO_TRANSLATION_ON_STARTUP`、`ARGO_TRANSLATION_SCHEDULED`、`ARGO_TRANSLATION_CRON` | 繁中翻譯同步（每週一 05:00） |
| `ARGO_RATELIMIT_ENABLED` | 是否限流（`true`） |
| `ARGO_RATELIMIT_GENERAL_PER_MINUTE` | 每 IP 每分鐘所有 API（`300`） |
| `ARGO_RATELIMIT_AUTH_PER_MINUTE` | 登入與註冊（`20`） |
| `ARGO_RATELIMIT_CHECKOUT_PER_MINUTE` | 下單與付款（`30`） |
| `ARGO_ADMIN_SESSION_HOURS`、`ARGO_ADMIN_MAX_FAILURES`、`ARGO_ADMIN_LOCK_MINUTES`、`ARGO_ADMIN_FAILURE_WINDOW_MINUTES` | 員工登入（`8`、`5`、`15`、`15`） |
| `ARGO_CUSTOMER_SESSION_DAYS`、`ARGO_CUSTOMER_MAX_FAILURES`、`ARGO_CUSTOMER_LOCK_MINUTES`、`ARGO_CUSTOMER_FAILURE_WINDOW_MINUTES` | 顧客登入（`14`、`5`、`15`、`15`） |
| `ARGO_CUSTOMER_VERIFY_HOURS`、`ARGO_CUSTOMER_RESEND_SECONDS`、`ARGO_CUSTOMER_RESET_MINUTES`、`ARGO_CUSTOMER_CODE_MINUTES`、`ARGO_CUSTOMER_MAX_CODE_ATTEMPTS` | 驗證信、重寄間隔、重設密碼、驗證碼 |
| `ARGO_MONITOR_*` | 監控取樣與預警門檻，見 `application.properties` |
| `ARGO_SECURITY_BLOCK_CACHE_MILLIS` | 封鎖名單快取毫秒（`5000`） |

限流、封鎖統計與監控資料都存在記憶體，目前只支援單一後端實例。

## 前端

每個前端是一包靜態檔：

```
cd frontend/official-site   # 或 admin、ops
npm ci
npx vite build              # 輸出到 dist/
```

- 三個前端要放在不同的網址（來源），並全部列入 `ARGO_CORS_ORIGINS`。
- 單頁應用程式：找不到的路徑要回 `index.html`。
- `/api` 要由反向代理轉給後端，前端程式裡用相對路徑呼叫。
- `deploy/web/Dockerfile` 會建置其中一個前端並放進 nginx，`APP` 參數指定是哪一個。
- `deploy/web/nginx.conf.template` 同時是反向代理範例，包含：
  - `/api` 轉發與 `X-Forwarded-For`
  - SPA fallback
  - 安全標頭：CSP、X-Frame-Options、Referrer-Policy、X-Content-Type-Options
  - 環境變數 `BACKEND_URL`（後端位址）、`FORM_ACTION`（官網付款時表單會送到的綠界網址，要寫進 CSP，正式環境改成綠界正式網域，例如 `https://payment.ecpay.com.tw`）
- 後端自己也會對 API 回應加安全標頭，但靜態檔的標頭只能由網頁伺服器提供。
- 若 HTTPS 在 nginx 終止，請另外在 HTTPS 的 server 區塊加上 `Strict-Transport-Security`。

## 備份與維運

- 備份資料庫。`staff_audit_log` 是稽核紀錄，資料庫觸發器禁止修改與刪除。
- 員工帳號不刪除（稽核紀錄會引用），要停用就把 `disabled` 設為 1。
- 檢查存活：`GET /api/ping`。
