package com.rsvp.notification.service;

import com.rsvp.common.enums.ReminderChannel;
import com.rsvp.notification.entity.Notification;
import com.rsvp.notification.repository.NotificationRepository;
import com.rsvp.user.entity.User;
import com.rsvp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InAppNotificationSender implements NotificationSender {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	@Override
	public ReminderChannel channel() {
		return ReminderChannel.IN_APP;
	}

	@Override
	public void send(ReminderNotification notification) {
		User user = userRepository.findById(notification.userId()).orElseThrow();
		Notification stored = new Notification();
		stored.setUser(user);
		stored.setReminderId(notification.reminderId());
		stored.setEventId(notification.eventId());
		stored.setTaskId(notification.taskId());
		stored.setTitle(notification.title());
		stored.setBody(notification.body());
		stored.setChannel(notification.channel());
		notificationRepository.save(stored);
	}
}
