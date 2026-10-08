package com.argo.common;

import org.springframework.http.HttpStatus;

// 全站錯誤碼唯一來源
public enum ErrorCode {

	// 通用
	BAD_REQUEST(1001, HttpStatus.BAD_REQUEST, "參數缺少或型別不對"),
	BAD_REQUEST_BODY(1002, HttpStatus.BAD_REQUEST, "請求本文無法解析"),
	VALIDATION_ERROR(1003, HttpStatus.BAD_REQUEST, "欄位驗證失敗，data 列出有問題的欄位"),
	INVALID_PAGING(1004, HttpStatus.BAD_REQUEST, "分頁參數不合法"),
	INVALID_SORT(1005, HttpStatus.BAD_REQUEST, "不支援的排序方式"),
	INVALID_IDS(1006, HttpStatus.BAD_REQUEST, "商品編號清單不合法"),
	INVALID_RANGE(1007, HttpStatus.BAD_REQUEST, "開始時間晚於結束時間"),

	// 會員與員工登入
	UNAUTHORIZED(2001, HttpStatus.UNAUTHORIZED, "會員未登入或登入已失效"),
	LOGIN_FAILED(2002, HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤，一律用這個碼不洩漏細節"),
	LOGIN_LOCKED(2003, HttpStatus.TOO_MANY_REQUESTS, "失敗次數過多而鎖定，data.retryAfterSeconds 為剩餘秒數"),
	EMAIL_TAKEN(2004, HttpStatus.CONFLICT, "註冊的 Email 已被使用"),
	ADMIN_UNAUTHORIZED(2005, HttpStatus.UNAUTHORIZED, "員工未登入或登入已失效"),
	ADMIN_FORBIDDEN(2006, HttpStatus.FORBIDDEN, "員工角色沒有此功能的權限"),
	USERNAME_TAKEN(2007, HttpStatus.CONFLICT, "註冊的帳號已被使用"),

	// 卡片與系列
	CARD_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "找不到卡片"),
	SET_NOT_FOUND(3002, HttpStatus.NOT_FOUND, "找不到系列"),

	// 訂單
	INVALID_QUANTITY(4001, HttpStatus.BAD_REQUEST, "商品數量不合法"),
	ITEM_NOT_FOUND(4002, HttpStatus.BAD_REQUEST, "訂單內有商品不存在"),
	ITEM_UNAVAILABLE(4003, HttpStatus.CONFLICT, "訂單內有商品無法購買，例如系列已下架"),
	INSUFFICIENT_STOCK(4004, HttpStatus.CONFLICT, "訂單內有商品庫存不足"),
	ORDER_NOT_FOUND(4005, HttpStatus.NOT_FOUND, "訂單編號與 Email 不符或不存在"),
	ORDER_NOT_PAYABLE(4006, HttpStatus.CONFLICT, "訂單狀態不可付款"),
	ORDER_NOT_CANCELLABLE(4007, HttpStatus.CONFLICT, "訂單狀態不可取消"),
	ORDER_EXPIRED(4008, HttpStatus.GONE, "付款期限已過，訂單已取消"),
	ORDER_STATE_CONFLICT(4009, HttpStatus.CONFLICT, "訂單目前狀態不可執行此操作"),

	// 付款
	INVALID_CARD(5001, HttpStatus.BAD_REQUEST, "信用卡號不正確"),
	CARD_EXPIRED(5002, HttpStatus.BAD_REQUEST, "信用卡已過期"),
	CARD_DECLINED(5003, HttpStatus.PAYMENT_REQUIRED, "發卡行拒絕交易"),
	INSUFFICIENT_FUNDS(5004, HttpStatus.PAYMENT_REQUIRED, "信用卡餘額不足"),
	PROCESSING_ERROR(5005, HttpStatus.PAYMENT_REQUIRED, "付款處理發生錯誤"),

	// IP 防護
	IP_BLOCKED(6001, HttpStatus.FORBIDDEN, "來源 IP 已被封鎖"),
	RATE_LIMITED(6002, HttpStatus.TOO_MANY_REQUESTS, "請求過於頻繁，data.retryAfterSeconds 為等待秒數"),
	INVALID_IP(6003, HttpStatus.BAD_REQUEST, "IP 或 CIDR 格式不正確"),
	PROTECTED_IP(6004, HttpStatus.BAD_REQUEST, "特殊位址或代理不可封鎖"),
	CANNOT_BLOCK_SELF(6005, HttpStatus.BAD_REQUEST, "不可封鎖操作者自己的 IP"),
	IP_NOT_BLOCKED(6006, HttpStatus.NOT_FOUND, "該 IP 目前沒有被封鎖"),
	RULE_NOT_FOUND(6007, HttpStatus.NOT_FOUND, "找不到自動封鎖規則"),
	ALLOW_NOT_FOUND(6008, HttpStatus.NOT_FOUND, "找不到白名單項目");

	private final int number;
	private final HttpStatus status;
	private final String description;

	ErrorCode(int number, HttpStatus status, String description) {
		this.number = number;
		this.status = status;
		this.description = description;
	}

	public int number() {
		return number;
	}

	public HttpStatus status() {
		return status;
	}

	public String description() {
		return description;
	}
}
