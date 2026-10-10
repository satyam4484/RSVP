package com.rsvp.contact.dto;

import com.rsvp.common.enums.ContactType;
import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
		UUID id,
		String name,
		String phone,
		String normalizedPhone,
		String email,
		String companyName,
		ContactType type,
		String notes,
		Instant createdAt,
		Instant updatedAt) {
}
