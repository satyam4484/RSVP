package com.rsvp.common.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcJobLock implements JobLock {

	private final JdbcTemplate jdbcTemplate;
	private final String owner = UUID.randomUUID().toString();

	public JdbcJobLock(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@Transactional
	public boolean tryLock(String jobName, Duration lease) {
		Instant now = Instant.now();
		Instant until = now.plus(lease);
		int updated = jdbcTemplate.update(
				"UPDATE scheduler_locks SET locked_until = ?, locked_by = ? WHERE name = ? AND locked_until <= ?",
				until, owner, jobName, now);
		if (updated == 1) {
			return true;
		}
		try {
			jdbcTemplate.update(
					"INSERT INTO scheduler_locks (name, locked_until, locked_by) VALUES (?, ?, ?)",
					jobName, until, owner);
			return true;
		} catch (DataIntegrityViolationException ex) {
			return false;
		}
	}

	@Override
	@Transactional
	public void unlock(String jobName) {
		jdbcTemplate.update(
				"UPDATE scheduler_locks SET locked_until = ? WHERE name = ? AND locked_by = ?",
				Instant.now().minusSeconds(1), jobName, owner);
	}
}
