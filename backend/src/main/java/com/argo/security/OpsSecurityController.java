package com.argo.security;

import com.argo.admin.AdminAuthInterceptor;
import com.argo.admin.RequirePermission;
import com.argo.admin.StaffAccount;
import com.argo.common.PageResult;
import com.argo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 運維後台：查看異常 IP、封鎖與解除封鎖
@RestController
@RequestMapping("/api/ops")
public class OpsSecurityController {

	private final IpAdminService admin;
	private final IpBlockService blocks;
	private final ClientIpResolver resolver;

	public OpsSecurityController(IpAdminService admin, IpBlockService blocks, ClientIpResolver resolver) {
		this.admin = admin;
		this.blocks = blocks;
		this.resolver = resolver;
	}

	@GetMapping("/ips")
	@RequirePermission("security.ips")
	public Result<PageResult<IpActivityView>> ips(@RequestParam(defaultValue = "7") int days,
			@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return Result.ok(admin.activity(days, keyword, page, size));
	}

	@GetMapping("/ip-blocks")
	@RequirePermission("security.ips")
	public Result<List<IpBlockView>> activeBlocks() {
		return Result.ok(admin.activeBlocks());
	}

	@PostMapping("/ip-blocks")
	@RequirePermission("security.ips.block")
	public Result<IpBlockView> block(@Valid @RequestBody BlockIpRequest body, HttpServletRequest req) {
		StaffAccount staff = (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
		return Result.ok(IpBlockView.from(blocks.block(staff, body.ip(), body.reason(), body.hours(), resolver.resolve(req))));
	}

	@DeleteMapping("/ip-blocks")
	@RequirePermission("security.ips.block")
	public Result<Void> unblock(@RequestParam String ip, HttpServletRequest req) {
		blocks.unblock((StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR), ip);
		return Result.ok();
	}
}
