package com.rsvp.document.dto;

import com.rsvp.common.enums.StorageProvider;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
		UUID id,
		String name,
		StorageProvider provider,
		String mimeType,
		Long size,
		UUID eventId,
		UUID travelId,
		UUID expenseId,
		UUID contactId,
		UUID taskId,
		Instant createdAt) {
}
