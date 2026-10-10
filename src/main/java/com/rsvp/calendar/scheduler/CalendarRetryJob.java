package com.rsvp.calendar.scheduler;

import com.rsvp.calendar.service.CalendarSyncService;
import com.rsvp.common.scheduling.JobLock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rsvp.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CalendarRetryJob {

	private final CalendarSyncService calendarSyncService;
	private final JobLock jobLock;

	@Scheduled(fixedDelayString = "${rsvp.scheduling.calendar-retry-delay-ms:600000}")
	public void retry() {
		if (!jobLock.tryLock("calendar-retry", Duration.ofMinutes(8))) {
			return;
		}
		try {
			calendarSyncService.retryFailed();
		} finally {
			jobLock.unlock("calendar-retry");
		}
	}
}
