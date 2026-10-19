package com.argo.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CustomerCleanerTests {

	@Autowired
	CustomerCleaner cleaner;
	@Autowired
	CustomerAuthService auth;
	@Autowired
	JdbcTemplate jdbc;
	@PersistenceContext
	EntityManager em;

	private int count(String table, String hash) {
		return jdbc.queryForObject("select count(*) from " + table + " where token_hash = ?", Integer.class, hash);
	}

	@Test
	void removesOnlyExpiredRows() {
		String email = "cleaner@test.local";
		auth.createVerified(TestUsers.of(email), email, "correct-horse-1", null);
		em.flush();
		Long id = jdbc.queryForObject("select id from customer_account where email = ?", Long.class, email);
		String past = "now() - interval '1 day'";
		String future = "now() + interval '1 day'";
		for (String[] r : new String[][] { { "t3-old", past }, { "t3-new", future } }) {
			jdbc.update("insert into customer_session (token_hash, customer_id, expires_at) values (?, ?, "
					+ r[1] + ")", r[0], id);
			jdbc.update("insert into customer_email_token (token_hash, customer_id, purpose, expires_at) "
					+ "values (?, ?, 'RESET', " + r[1] + ")", r[0], id);
			jdbc.update("insert into customer_pending_registration (token_hash, username, email, password_hash, "
					+ "expires_at, code_hash, code_expires_at) values (?, ?, ?, 'x', " + r[1] + ", 'c', now())",
					r[0], "u-" + r[0], r[0] + "@test.local");
		}
		em.clear();
		cleaner.purge();
		for (String table : new String[] { "customer_session", "customer_email_token",
				"customer_pending_registration" }) {
			assertEquals(0, count(table, "t3-old"), table);
			assertEquals(1, count(table, "t3-new"), table);
		}
	}
}
