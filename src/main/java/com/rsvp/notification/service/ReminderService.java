package com.rsvp.notification.service;

import com.rsvp.common.enums.ReminderStatus;
import com.rsvp.notification.dto.NotificationResponse;
import com.rsvp.notification.repository.NotificationRepository;
import com.rsvp.task.entity.Reminder;
import com.rsvp.task.repository.ReminderRepository;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.common.util.PageRequests;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderService {

	private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

	private final ReminderRepository reminderRepository;
	private final NotificationRepository notificationRepository;
	private final List<NotificationSender> senders;
	private final Clock clock;

	@Transactional
	public int dispatchDue() {
		List<Reminder> due = reminderRepository.findTop100ByStatusAndReminderAtLessThanEqualOrderByReminderAtAsc(
				ReminderStatus.PENDING, clock.instant());
		int sent = 0;
		for (Reminder reminder : due) {
			NotificationSender sender = senders.stream().filter(item -> item.channel() == reminder.getChannel()).findFirst()
					.orElse(null);
			if (sender == null) {
				reminder.setStatus(ReminderStatus.FAILED);
				reminder.setErrorMessage("Channel is not configured");
				log.info("operation=reminder provider={} success=false channel={}", "notification", reminder.getChannel());
				continue;
			}
			try {
				sender.send(new ReminderNotification(reminder.getEvent().getUser().getId(), reminder.getId(),
						reminder.getEvent().getId(), reminder.getTask().getId(), reminder.getTask().getTitle(),
						reminder.getTask().getDescription(), reminder.getChannel()));
				reminder.setStatus(ReminderStatus.SENT);
				reminder.setSentAt(clock.instant());
				reminder.setErrorMessage(null);
				sent++;
				log.info("operation=reminder provider={} success=true channel={}", "notification", reminder.getChannel());
			} catch (RuntimeException ex) {
				reminder.setStatus(ReminderStatus.FAILED);
				reminder.setErrorMessage("Delivery failed");
				log.warn("operation=reminder provider={} success=false channel={}", "notification", reminder.getChannel());
			}
		}
		return sent;
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> list(Long userId, boolean unreadOnly, int page, int size) {
		var pageable = PageRequests.of(page, size, "createdAt,desc", Set.of("createdAt"), "createdAt,desc");
		Page<com.rsvp.notification.entity.Notification> result = unreadOnly
				? notificationRepository.findByUser_IdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
				: notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
		return result.map(this::toResponse);
	}

	@Transactional
	public NotificationResponse markRead(Long userId, UUID notificationId) {
		var notification = notificationRepository.findByUuidAndUser_Id(notificationId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found"));
		if (notification.getReadAt() == null) {
			notification.setReadAt(clock.instant());
		}
		return toResponse(notification);
	}

	private NotificationResponse toResponse(com.rsvp.notification.entity.Notification notification) {
		return new NotificationResponse(notification.getUuid(), notification.getTitle(), notification.getBody(),
				notification.getChannel(), notification.getReadAt() != null, notification.getCreatedAt());
	}
}
