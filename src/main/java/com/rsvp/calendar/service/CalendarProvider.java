package com.rsvp.calendar.service;

import com.rsvp.common.enums.CalendarProviderType;
import java.time.Instant;
import java.util.List;

public interface CalendarProvider {

	CalendarProviderType type();

	default boolean supports(CalendarProviderType provider) {
		return type() == provider;
	}

	ExternalCalendarEvent upsert(Long connectionId, CalendarEventRequest request);

	void delete(Long connectionId, String externalEventId, String idempotencyKey);

	List<ExternalCalendar> listCalendars(Long userId);

	record CalendarEventRequest(
			String idempotencyKey,
			String externalEventId,
			String summary,
			String description,
			String location,
			Instant start,
			Instant end,
			String timezone,
			String status) {
	}

	record ExternalCalendarEvent(String id, String url) {
	}

	record ExternalCalendar(String id, String name, boolean primary) {
	}
}
