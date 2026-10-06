package com.argo.common;

// 是否類欄位對外一律用 0 與 1
public final class Flag {

	private Flag() {
	}

	public static int of(boolean on) {
		return on ? 1 : 0;
	}
}
