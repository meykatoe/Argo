package com.argo.security;

// 可以觸發自動封鎖的事件
public enum AutoBlockMetric {
	// 被限速擋下
	RATE_LIMITED,
	// 登入或註冊失敗
	LOGIN_FAILED
}
