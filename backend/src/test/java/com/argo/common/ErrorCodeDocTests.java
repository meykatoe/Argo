package com.argo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

// 文件由列舉產生，不一致就失敗
// 重新產生：-Dargo.docs.write=true
class ErrorCodeDocTests {

	private static final Path DOC = Path.of("..", "docs", "error-codes.md");

	private static final Map<Integer, String> MODULES = Map.of(
			1, "通用", 2, "帳號與登入", 3, "卡片與系列", 4, "訂單", 5, "付款", 6, "IP 防護");

	static String render() {
		StringBuilder sb = new StringBuilder();
		sb.append("# 錯誤碼對照表\n\n");
		sb.append("本文件由 `ErrorCode` 列舉產生，請勿手改。新增或修改錯誤碼後，執行後端測試會提示更新。\n\n");
		sb.append("## 回應格式\n\n");
		sb.append("失敗時 HTTP 狀態維持真實值，本文件的「錯誤編號」放在回應的 `code` 欄位：\n\n");
		sb.append("```json\n{ \"code\": 4006, \"msg\": \"ORDER_NOT_PAYABLE\", \"data\": null }\n```\n\n");
		sb.append("- `code`：四位數錯誤編號，客服回報問題時使用。\n");
		sb.append("- `msg`：錯誤碼名稱，前端依它顯示翻譯後的訊息。\n");
		sb.append("- `data`：欄位細節，沒有則為 null。\n");
		sb.append("- 成功時 `code` 為 200、`msg` 為 `OK`。\n\n");
		sb.append("編號首位代表模組，後三位為流水號。\n");
		for (Map.Entry<Integer, String> m : new java.util.TreeMap<>(MODULES).entrySet()) {
			sb.append("\n## ").append(m.getKey()).append("xxx ").append(m.getValue()).append("\n\n");
			sb.append("| 錯誤編號 | 錯誤碼 | HTTP 狀態 | 說明 |\n|---|---|---|---|\n");
			for (ErrorCode c : ErrorCode.values()) {
				if (c.number() / 1000 == m.getKey()) {
					sb.append("| ").append(c.number()).append(" | `").append(c.name()).append("` | ")
							.append(c.status().value()).append(" | ").append(c.description()).append(" |\n");
				}
			}
		}
		return sb.toString();
	}

	@Test
	void everyCodeBelongsToAModule() {
		for (ErrorCode c : ErrorCode.values()) {
			assertEquals(true, MODULES.containsKey(c.number() / 1000), c.name());
		}
	}

	@Test
	void docMatchesEnum() throws IOException {
		String expected = render();
		if (Boolean.getBoolean("argo.docs.write")) {
			Files.createDirectories(DOC.getParent());
			Files.writeString(DOC, expected);
		}
		assertEquals(expected, Files.readString(DOC),
				"docs/error-codes.md 過期，用 -Dargo.docs.write=true 重新產生");
	}
}
