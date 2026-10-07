package com.argo.common;

// 統一回應格式，失敗時 code 為錯誤編號
// msg 為錯誤碼，data 放細節
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
