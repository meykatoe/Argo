package com.argo.staff.auth;

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
class StaffSessionCleanerTests {

	@Autowired
	StaffSessionCleaner cleaner;
	@Autowired
	StaffAuthService auth;
	@Autowired
	JdbcTemplate jdbc;
	@PersistenceContext
	EntityManager em;

	private int count(String hash) {
		return jdbc.queryForObject("select count(*) from staff_session where token_hash = ?", Integer.class, hash);
	}

	@Test
	void removesOnlyExpiredSessions() {
		Long id = auth.create("cleaner-t3", "Password-1234", StaffRole.ADMIN).getId();
		jdbc.update("insert into staff_session (token_hash, staff_id, expires_at) "
				+ "values ('t3-staff-old', ?, now() - interval '1 day')", id);
		jdbc.update("insert into staff_session (token_hash, staff_id, expires_at) "
				+ "values ('t3-staff-new', ?, now() + interval '1 day')", id);
		em.clear();
		cleaner.purge();
		assertEquals(0, count("t3-staff-old"));
		assertEquals(1, count("t3-staff-new"));
	}
}
