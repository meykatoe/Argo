package com.argo.security;

import com.argo.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 前端啟動時用來確認自己有沒有被封鎖
@RestController
public class PingController {

	@GetMapping("/api/ping")
	public Result<Void> ping() {
		return Result.ok();
	}
}
