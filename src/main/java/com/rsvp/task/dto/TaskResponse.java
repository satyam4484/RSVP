package com.rsvp.task.dto;

import com.rsvp.common.enums.TaskPriority;
import com.rsvp.common.enums.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
		UUID id,
		UUID eventId,
		String title,
		String description,
		TaskStatus status,
		TaskPriority priority,
		Instant dueAt,
		Instant completedAt,
		UUID assignedToUserId,
		UUID contactId,
		boolean reminderEnabled,
		Instant reminderAt,
		Instant createdAt,
		Instant updatedAt) {
}
