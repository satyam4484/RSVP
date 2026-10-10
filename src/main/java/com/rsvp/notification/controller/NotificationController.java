package com.rsvp.notification.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.response.PagedResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.notification.dto.NotificationResponse;
import com.rsvp.notification.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

	private final ReminderService reminderService;

	@GetMapping
	@Operation(summary = "List in-app notifications")
	@StandardApiErrors
	public PagedResponse<NotificationResponse> list(
			@RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PagedResponse.from(reminderService.list(SecurityUtils.currentUserId(), unreadOnly, page, size));
	}

	@PatchMapping("/{notificationId}/read")
	@Operation(summary = "Mark a notification as read")
	@StandardApiErrors
	public ApiResponse<NotificationResponse> read(@PathVariable UUID notificationId) {
		return ApiResponse.of(reminderService.markRead(SecurityUtils.currentUserId(), notificationId), "Notification read");
	}
}
