package com.rsvp.event.dto;

import com.rsvp.common.enums.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
		UUID id,
		String name,
		String description,
		Instant startDateTime,
		Instant endDateTime,
		String timezone,
		String locationName,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		EventStatus status,
		String color,
		String notes,
		Instant completedAt,
		Instant archivedAt,
		Instant createdAt,
		Instant updatedAt) {
}
