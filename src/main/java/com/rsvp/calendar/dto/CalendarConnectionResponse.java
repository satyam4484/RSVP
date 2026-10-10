package com.rsvp.calendar.dto;

import com.rsvp.common.enums.CalendarProviderType;
import com.rsvp.common.enums.SyncStatus;
import java.time.Instant;
import java.util.UUID;

public record CalendarConnectionResponse(
		UUID id,
		CalendarProviderType provider,
		String externalCalendarId,
		String calendarName,
		boolean isDefault,
		boolean enabled,
		SyncStatus syncStatus,
		Instant lastSyncedAt,
		String subscriptionUrl) {
}
