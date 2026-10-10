package com.rsvp.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DriveDocumentRequest(
		@NotBlank @Size(max = 500) String externalFileId,
		UUID eventId,
		UUID travelId,
		UUID expenseId,
		UUID contactId,
		UUID taskId,
		@Size(max = 255) String name) {
}
