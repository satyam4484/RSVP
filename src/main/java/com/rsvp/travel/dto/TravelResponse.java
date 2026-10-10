package com.rsvp.travel.dto;

import com.rsvp.common.enums.TravelType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TravelResponse(
		UUID id,
		UUID eventId,
		TravelType type,
		String title,
		String fromLocation,
		String toLocation,
		Instant departureAt,
		Instant arrivalAt,
		String timezone,
		String bookingReference,
		String providerName,
		String notes,
		Map<String, Object> details,
		Instant createdAt,
		Instant updatedAt) {
}
