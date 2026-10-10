package com.rsvp.calendar.service;

import com.rsvp.common.event.CalendarSyncRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CalendarSyncListener {

	private final CalendarSyncService calendarSyncService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCalendarSync(CalendarSyncRequested request) {
		calendarSyncService.synchronize(request);
	}
}
