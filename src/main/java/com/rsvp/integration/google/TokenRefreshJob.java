package com.rsvp.integration.google;

import com.rsvp.common.scheduling.JobLock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rsvp.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TokenRefreshJob {

	private final GoogleAccountService googleAccountService;
	private final JobLock jobLock;

	@Scheduled(fixedDelayString = "${rsvp.scheduling.token-refresh-delay-ms:900000}")
	public void refresh() {
		if (!jobLock.tryLock("token-refresh", Duration.ofMinutes(8))) {
			return;
		}
		try {
			googleAccountService.refreshExpiringTokens();
		} finally {
			jobLock.unlock("token-refresh");
		}
	}
}
