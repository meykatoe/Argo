package com.argo.staff.auth;

import com.argo.staff.audit.AuditAction;
import com.argo.staff.audit.AuditLogService;
import java.io.Console;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

// 以指令建立人員帳號後結束程式
@Component
@ConditionalOnProperty(name = "argo.staff.create")
public class StaffAccountRunner implements ApplicationRunner {

	private final StaffAuthService auth;
	private final AuditLogService audit;
	private final ConfigurableApplicationContext ctx;

	public StaffAccountRunner(StaffAuthService auth, AuditLogService audit,
			ConfigurableApplicationContext ctx) {
		this.auth = auth;
		this.audit = audit;
		this.ctx = ctx;
	}

	@Override
	public void run(ApplicationArguments args) {
		int code = 0;
		try {
			String username = args.getOptionValues("argo.staff.create").get(0);
			var roleArg = args.getOptionValues("argo.staff.role");
			if (roleArg == null) {
				throw new IllegalArgumentException("--argo.staff.role is required (ADMIN, GENERAL, SERVICE)");
			}
			StaffRole role = StaffRole.valueOf(roleArg.get(0).toUpperCase());
			StaffAccount s = auth.create(username, readPassword(), role);
			// 無登入者，記錄執行指令的系統使用者
			audit.record(null, "cli:" + System.getProperty("user.name"), AuditAction.ACCOUNT_CREATED,
					true, "STAFF", s.getUsername(), Map.of("role", s.getRole().name()));
			System.out.println("已建立帳號 " + s.getUsername() + "，權限 " + s.getRole());
		} catch (RuntimeException e) {
			System.err.println("建立失敗：" + e.getMessage());
			code = 1;
		}
		int exit = code;
		System.exit(SpringApplication.exit(ctx, () -> exit));
	}

	// 密碼不放在指令列，避免留在歷史紀錄
	private String readPassword() {
		String env = System.getenv("STAFF_PASSWORD");
		if (env != null && !env.isEmpty()) {
			return env;
		}
		Console console = System.console();
		if (console == null) {
			throw new IllegalArgumentException("set STAFF_PASSWORD or run in a terminal");
		}
		return new String(console.readPassword("密碼："));
	}
}
