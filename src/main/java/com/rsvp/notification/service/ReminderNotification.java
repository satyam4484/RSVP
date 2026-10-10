package com.rsvp.notification.service;

import com.rsvp.common.enums.ReminderChannel;
import com.rsvp.notification.entity.Notification;

public record ReminderNotification(Long userId, Long reminderId, Long eventId, Long taskId, String title, String body,
		ReminderChannel channel) {
}
