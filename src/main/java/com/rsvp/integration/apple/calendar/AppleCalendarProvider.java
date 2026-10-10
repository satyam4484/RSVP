package com.rsvp.integration.apple.calendar;

import com.rsvp.calendar.service.CalendarProvider;
import com.rsvp.common.enums.CalendarProviderType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AppleCalendarProvider implements CalendarProvider {

	@Override
	public CalendarProviderType type() {
		return CalendarProviderType.APPLE;
	}

	@Override
	public boolean supports(CalendarProviderType provider) {
		return provider == CalendarProviderType.APPLE || provider == CalendarProviderType.ICAL;
	}

	@Override
	public ExternalCalendarEvent upsert(Long connectionId, CalendarEventRequest request) {
		return new ExternalCalendarEvent(request.idempotencyKey(), null);
	}

	@Override
	public void delete(Long connectionId, String externalEventId, String idempotencyKey) {
		// The Apple subscription feed is generated from RSVP, so removing the source item removes it from the feed.
	}

	@Override
	public List<ExternalCalendar> listCalendars(Long userId) {
		return List.of(new ExternalCalendar("rsvp-feed", "RSVP Events", true));
	}
}
