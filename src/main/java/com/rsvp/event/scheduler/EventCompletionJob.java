package com.rsvp.event.scheduler;

import com.rsvp.common.scheduling.JobLock;
import com.rsvp.event.service.EventService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rsvp.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventCompletionJob {

	private static final Logger log = LoggerFactory.getLogger(EventCompletionJob.class);

	private final EventService eventService;
	private final JobLock jobLock;

	@Scheduled(fixedDelayString = "${rsvp.scheduling.event-completion-delay-ms:600000}")
	public void completeElapsedEvents() {
		if (!jobLock.tryLock("event-completion", Duration.ofMinutes(8))) {
			return;
		}
		try {
			int completed = eventService.autoCompleteDueEvents();
			if (completed > 0) {
				log.info("operation=event-auto-complete success=true count={}", completed);
			}
		} finally {
			jobLock.unlock("event-completion");
		}
	}
}
