package com.rsvp.notification.dto;

import com.rsvp.common.enums.ReminderChannel;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		String title,
		String body,
		ReminderChannel channel,
		boolean read,
		Instant createdAt) {
}
