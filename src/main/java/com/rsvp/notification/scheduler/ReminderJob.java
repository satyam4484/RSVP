package com.rsvp.notification.scheduler;

import com.rsvp.common.scheduling.JobLock;
import com.rsvp.notification.service.ReminderService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rsvp.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReminderJob {

	private final ReminderService reminderService;
	private final JobLock jobLock;

	@Scheduled(fixedDelayString = "${rsvp.scheduling.reminder-delay-ms:60000}")
	public void dispatch() {
		if (!jobLock.tryLock("reminders", Duration.ofMinutes(2))) {
			return;
		}
		try {
			reminderService.dispatchDue();
		} finally {
			jobLock.unlock("reminders");
		}
	}
}
