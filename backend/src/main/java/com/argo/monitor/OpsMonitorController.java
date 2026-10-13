package com.argo.monitor;

import com.argo.common.Result;
import com.argo.staff.auth.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 系統監控
@RestController
@RequestMapping("/api/ops")
public class OpsMonitorController {

	private final MonitorService service;

	public OpsMonitorController(MonitorService service) {
		this.service = service;
	}

	@GetMapping("/monitor")
	@RequirePermission("system.monitor")
	public Result<MonitorView> monitor(@RequestParam(defaultValue = "5") int minutes) {
		return Result.ok(service.view(minutes));
	}
}
