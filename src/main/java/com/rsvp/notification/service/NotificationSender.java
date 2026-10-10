package com.rsvp.notification.service;

import com.rsvp.common.enums.ReminderChannel;

public interface NotificationSender {

	ReminderChannel channel();

	void send(ReminderNotification notification);
}
