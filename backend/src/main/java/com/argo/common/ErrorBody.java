package com.argo.common;

import java.util.Map;

public record ErrorBody(String code, int status, Map<String, String> details) {

	public ErrorBody(String code, int status) {
		this(code, status, Map.of());
	}
}
