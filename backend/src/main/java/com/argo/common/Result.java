package com.argo.common;

// 後端回傳前端的統一格式，失敗時 msg 為錯誤代碼、data 放欄位細節
public record Result<T>(int code, String msg, T data) {

	public static final int OK = 200;

	public static <T> Result<T> ok(T data) {
		return new Result<>(OK, "OK", data);
	}

	public static Result<Void> ok() {
		return new Result<>(OK, "OK", null);
	}

	public static <T> Result<T> error(int code, String msg, T data) {
		return new Result<>(code, msg, data);
	}
}
