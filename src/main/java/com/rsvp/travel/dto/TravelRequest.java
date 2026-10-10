package com.rsvp.travel.dto;

import com.rsvp.common.enums.TravelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

public record TravelRequest(
		@NotNull TravelType type,
		@NotBlank @Size(max = 200) String title,
		@Size(max = 255) String fromLocation,
		@Size(max = 255) String toLocation,
		Instant departureAt,
		Instant arrivalAt,
		@NotBlank @Size(max = 64) String timezone,
		@Size(max = 100) String bookingReference,
		@Size(max = 200) String providerName,
		@Size(max = 5000) String notes,
		Map<String, Object> details) {
}
