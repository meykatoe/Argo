package com.argo.security.web;

import com.argo.common.PageResult;
import com.argo.common.Result;
import com.argo.security.block.AllowEntryView;
import com.argo.security.block.AutoBlockMetric;
import com.argo.security.block.AutoBlockPolicyService;
import com.argo.security.block.AutoBlockRuleView;
import com.argo.security.block.IpActivityView;
import com.argo.security.block.IpAdminService;
import com.argo.security.block.IpBlockService;
import com.argo.security.block.IpBlockView;
import com.argo.security.block.IpRulesView;
import com.argo.security.net.ClientIpResolver;
import com.argo.staff.auth.AdminAuthInterceptor;
import com.argo.staff.auth.RequirePermission;
import com.argo.staff.auth.StaffAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final AutoBlockPolicyService policy;

	public OpsSecurityController(IpAdminService admin, IpBlockService blocks, ClientIpResolver resolver,
			AutoBlockPolicyService policy) {
		this.policy = policy;
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

	// 自動封鎖規則與白名單
	@GetMapping("/ip-rules")
	@RequirePermission("security.rules")
	public Result<IpRulesView> rules() {
		return Result.ok(new IpRulesView(policy.listRules().stream().map(AutoBlockRuleView::from).toList(),
				policy.listAllow().stream().map(AllowEntryView::from).toList()));
	}

	@PatchMapping("/ip-rules/{metric}")
	@RequirePermission("security.rules.edit")
	public Result<AutoBlockRuleView> updateRule(@PathVariable AutoBlockMetric metric,
			@Valid @RequestBody UpdateRuleRequest body, HttpServletRequest req) {
		return Result.ok(AutoBlockRuleView.from(policy.updateRule(staff(req), metric, body.enabled() == 1,
				body.threshold(), body.windowMinutes(), body.blockHours())));
	}

	@PostMapping("/ip-allowlist")
	@RequirePermission("security.rules.edit")
	public Result<AllowEntryView> addAllow(@Valid @RequestBody AddAllowRequest body, HttpServletRequest req) {
		return Result.ok(AllowEntryView.from(policy.addAllow(staff(req), body.ip(), body.note())));
	}

	@DeleteMapping("/ip-allowlist")
	@RequirePermission("security.rules.edit")
	public Result<Void> removeAllow(@RequestParam String ip, HttpServletRequest req) {
		policy.removeAllow(staff(req), ip);
		return Result.ok();
	}

	private static StaffAccount staff(HttpServletRequest req) {
		return (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
	}
}
