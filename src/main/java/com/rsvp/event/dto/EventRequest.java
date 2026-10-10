package com.rsvp.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record EventRequest(
		@NotBlank @Size(max = 200) String name,
		@Size(max = 5000) String description,
		@NotNull Instant startDateTime,
		@NotNull Instant endDateTime,
		@NotBlank @Size(max = 64) String timezone,
		@Size(max = 255) String locationName,
		@Size(max = 500) String address,
		BigDecimal latitude,
		BigDecimal longitude,
		@Size(max = 32) String color,
		@Size(max = 5000) String notes) {
}
