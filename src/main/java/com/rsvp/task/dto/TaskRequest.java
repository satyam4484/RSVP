package com.rsvp.task.dto;

import com.rsvp.common.enums.ReminderChannel;
import com.rsvp.common.enums.TaskPriority;
import com.rsvp.common.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record TaskRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 5000) String description,
		TaskStatus status,
		TaskPriority priority,
		Instant dueAt,
		UUID assignedToUserId,
		UUID contactId,
		Boolean reminderEnabled,
		Instant reminderAt,
		ReminderChannel reminderChannel) {
}
