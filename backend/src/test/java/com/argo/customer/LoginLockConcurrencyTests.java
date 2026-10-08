package com.argo.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.argo.common.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

// 同時猜密碼也不能多猜：這裡不用交易回滾，真的提交後再清掉
@SpringBootTest
@TestPropertySource(properties = "argo.customer.max-failures=3")
class LoginLockConcurrencyTests {

	@Autowired
	CustomerAuthService auth;
	@Autowired
	JdbcTemplate jdbc;

	@Test
	void parallelGuessesAreCountedExactly() throws Exception {
		String email = "parallel-lock@test.local";
		auth.register(TestUsers.of(email), email, "correct-horse-1", null);
		int threads = 12;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch go = new CountDownLatch(1);
		try {
			List<Future<String>> results = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				results.add(pool.submit(() -> {
					go.await();
					try {
						auth.login(email, "wrong-password");
						return "OK";
					} catch (ApiException e) {
						return e.getCode();
					}
				}));
			}
			go.countDown();
			int failed = 0;
			int locked = 0;
			for (Future<String> f : results) {
				String r = f.get();
				if (r.equals("LOGIN_FAILED")) failed++;
				if (r.equals("LOGIN_LOCKED")) locked++;
			}
			// 只允許 3 次錯誤，其餘都被鎖定
			assertEquals(3, failed);
			assertEquals(threads - 3, locked);
		} finally {
			pool.shutdownNow();
			jdbc.update("delete from customer_account where email = ?", email);
		}
	}
}
