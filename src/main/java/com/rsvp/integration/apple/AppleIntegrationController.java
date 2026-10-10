package com.rsvp.integration.apple;

import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.common.config.RsvpProperties;
import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.enums.CalendarProviderType;
import com.rsvp.common.enums.SyncStatus;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.common.util.TokenHasher;
import com.rsvp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/apple")
@RequiredArgsConstructor
@Tag(name = "Apple integration")
public class AppleIntegrationController {

	private final CalendarConnectionRepository calendarConnectionRepository;
	private final UserService userService;
	private final RsvpProperties properties;

	@PostMapping("/connect")
	@Transactional
	@Operation(summary = "Create an iCalendar subscription URL for Apple Calendar. RSVP does not ask for an Apple password.")
	@StandardApiErrors
	public ApiResponse<Map<String, String>> connect() {
		Long userId = SecurityUtils.currentUserId();
		CalendarConnection connection = calendarConnectionRepository.findByUser_IdAndProvider(userId, CalendarProviderType.APPLE)
				.orElseGet(CalendarConnection::new);
		connection.setUser(userService.requireActive(userId));
		connection.setProvider(CalendarProviderType.APPLE);
		connection.setExternalCalendarId("rsvp-feed");
		connection.setCalendarName("RSVP Events");
		connection.setDefault(false);
		connection.setEnabled(true);
		connection.setSyncStatus(SyncStatus.SYNCED);
		if (connection.getFeedToken() == null) {
			connection.setFeedToken(TokenHasher.sha256(TokenHasher.randomToken()));
		}
		CalendarConnection saved = calendarConnectionRepository.save(connection);
		String url = properties.getPublicBaseUrl() + "/api/v1/calendar/feeds/" + saved.getFeedToken() + ".ics";
		return ApiResponse.of(Map.of(
				"provider", "APPLE",
				"subscriptionUrl", url,
				"instructions", "Subscribe to this URL in Apple Calendar. RSVP remains the source of truth."),
				"Apple calendar subscription created");
	}

	@DeleteMapping
	@Transactional
	@Operation(summary = "Disable the Apple calendar subscription")
	@StandardApiErrors
	public ApiResponse<Void> disconnect() {
		calendarConnectionRepository.findByUser_IdAndProvider(SecurityUtils.currentUserId(), CalendarProviderType.APPLE)
				.ifPresent(connection -> {
					connection.setEnabled(false);
					connection.setSyncStatus(SyncStatus.NOT_CONNECTED);
				});
		return ApiResponse.message("Apple calendar disconnected");
	}
}
