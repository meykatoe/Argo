# 錯誤碼對照表

本文件由 `ErrorCode` 列舉產生，請勿手改。新增或修改錯誤碼後，執行後端測試會提示更新。

## 回應格式

失敗時 HTTP 狀態維持真實值，本文件的「錯誤編號」放在回應的 `code` 欄位：

```json
{ "code": 4006, "msg": "ORDER_NOT_PAYABLE", "data": null }
```

- `code`：四位數錯誤編號，客服回報問題時使用。
- `msg`：錯誤碼名稱，前端依它顯示翻譯後的訊息。
- `data`：欄位細節，沒有則為 null。
- 成功時 `code` 為 200、`msg` 為 `OK`。

編號首位代表模組，後三位為流水號。

## 1xxx 通用

| 錯誤編號 | 錯誤碼 | HTTP 狀態 | 說明 |
|---|---|---|---|
| 1001 | `BAD_REQUEST` | 400 | 參數缺少或型別不對 |
| 1002 | `BAD_REQUEST_BODY` | 400 | 請求本文無法解析 |
| 1003 | `VALIDATION_ERROR` | 400 | 欄位驗證失敗，data 列出有問題的欄位 |
| 1004 | `INVALID_PAGING` | 400 | 分頁參數不合法 |
| 1005 | `INVALID_SORT` | 400 | 不支援的排序方式 |
| 1006 | `INVALID_IDS` | 400 | 商品編號清單不合法 |
| 1007 | `INVALID_RANGE` | 400 | 開始時間晚於結束時間 |

## 2xxx 帳號與登入

| 錯誤編號 | 錯誤碼 | HTTP 狀態 | 說明 |
|---|---|---|---|
| 2001 | `UNAUTHORIZED` | 401 | 會員未登入或登入已失效 |
| 2002 | `LOGIN_FAILED` | 401 | 帳號或密碼錯誤，一律用這個碼不洩漏細節 |
| 2003 | `LOGIN_LOCKED` | 429 | 失敗次數過多而鎖定，data.retryAfterSeconds 為剩餘秒數 |
| 2004 | `EMAIL_TAKEN` | 409 | 註冊的 Email 已被使用 |
| 2005 | `ADMIN_UNAUTHORIZED` | 401 | 員工未登入或登入已失效 |
| 2006 | `ADMIN_FORBIDDEN` | 403 | 員工角色沒有此功能的權限 |
| 2007 | `USERNAME_TAKEN` | 409 | 註冊的帳號已被使用 |

## 3xxx 卡片與系列

| 錯誤編號 | 錯誤碼 | HTTP 狀態 | 說明 |
|---|---|---|---|
| 3001 | `CARD_NOT_FOUND` | 404 | 找不到卡片 |
| 3002 | `SET_NOT_FOUND` | 404 | 找不到系列 |

## 4xxx 訂單

| 錯誤編號 | 錯誤碼 | HTTP 狀態 | 說明 |
|---|---|---|---|
| 4001 | `INVALID_QUANTITY` | 400 | 商品數量不合法 |
| 4002 | `ITEM_NOT_FOUND` | 400 | 訂單內有商品不存在 |
| 4003 | `ITEM_UNAVAILABLE` | 409 | 訂單內有商品無法購買，例如系列已下架 |
| 4004 | `INSUFFICIENT_STOCK` | 409 | 訂單內有商品庫存不足 |
| 4005 | `ORDER_NOT_FOUND` | 404 | 訂單編號與 Email 不符或不存在 |
| 4006 | `ORDER_NOT_PAYABLE` | 409 | 訂單狀態不可付款 |
| 4007 | `ORDER_NOT_CANCELLABLE` | 409 | 訂單狀態不可取消 |
| 4008 | `ORDER_EXPIRED` | 410 | 付款期限已過，訂單已取消 |
| 4009 | `ORDER_STATE_CONFLICT` | 409 | 訂單目前狀態不可執行此操作 |

## 5xxx 付款

| 錯誤編號 | 錯誤碼 | HTTP 狀態 | 說明 |
|---|---|---|---|
| 5001 | `INVALID_CARD` | 400 | 信用卡號不正確 |
| 5002 | `CARD_EXPIRED` | 400 | 信用卡已過期 |
| 5003 | `CARD_DECLINED` | 402 | 發卡行拒絕交易 |
| 5004 | `INSUFFICIENT_FUNDS` | 402 | 信用卡餘額不足 |
| 5005 | `PROCESSING_ERROR` | 402 | 付款處理發生錯誤 |

## 6xxx IP 防護

| 錯誤編號 | 錯誤碼 | HTTP 狀態 | 說明 |
|---|---|---|---|
| 6001 | `IP_BLOCKED` | 403 | 來源 IP 已被封鎖 |
| 6002 | `RATE_LIMITED` | 429 | 請求過於頻繁，data.retryAfterSeconds 為等待秒數 |
| 6003 | `INVALID_IP` | 400 | IP 或 CIDR 格式不正確 |
| 6004 | `PROTECTED_IP` | 400 | 特殊位址或代理不可封鎖 |
| 6005 | `CANNOT_BLOCK_SELF` | 400 | 不可封鎖操作者自己的 IP |
| 6006 | `IP_NOT_BLOCKED` | 404 | 該 IP 目前沒有被封鎖 |
| 6007 | `RULE_NOT_FOUND` | 404 | 找不到自動封鎖規則 |
| 6008 | `ALLOW_NOT_FOUND` | 404 | 找不到白名單項目 |
