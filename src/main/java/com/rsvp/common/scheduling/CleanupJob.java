package com.rsvp.common.scheduling;

import com.rsvp.auth.repository.RefreshTokenRepository;
import com.rsvp.integration.repository.OAuthStateRepository;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rsvp.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CleanupJob {

	private final RefreshTokenRepository refreshTokenRepository;
	private final OAuthStateRepository oauthStateRepository;
	private final JobLock jobLock;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${rsvp.scheduling.cleanup-delay-ms:86400000}")
	@Transactional
	public void cleanup() {
		if (!jobLock.tryLock("cleanup", Duration.ofMinutes(10))) {
			return;
		}
		try {
			refreshTokenRepository.deleteExpiredBefore(clock.instant());
			oauthStateRepository.deleteExpiredBefore(clock.instant());
		} finally {
			jobLock.unlock("cleanup");
		}
	}
}
