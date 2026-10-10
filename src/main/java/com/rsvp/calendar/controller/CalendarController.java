package com.rsvp.calendar.controller;

import com.rsvp.calendar.dto.CalendarConnectionResponse;
import com.rsvp.calendar.dto.SelectCalendarRequest;
import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.calendar.service.CalendarProvider;
import com.rsvp.calendar.service.CalendarSyncService;
import com.rsvp.calendar.service.IcsService;
import com.rsvp.common.config.RsvpProperties;
import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.event.service.EventService;
import com.rsvp.task.service.TaskService;
import com.rsvp.travel.service.TravelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Calendar")
public class CalendarController {

	private final CalendarConnectionRepository calendarConnectionRepository;
	private final CalendarSyncService calendarSyncService;
	private final IcsService icsService;
	private final List<CalendarProvider> providers;
	private final RsvpProperties properties;
	private final EventService eventService;
	private final TravelService travelService;
	private final TaskService taskService;

	@GetMapping("/api/v1/calendar/connections")
	@Operation(summary = "List calendar connections")
	@StandardApiErrors
	public ApiResponse<List<CalendarConnectionResponse>> connections() {
		Long userId = SecurityUtils.currentUserId();
		return ApiResponse.of(calendarConnectionRepository.findByUser_Id(userId).stream().map(this::toResponse).toList(),
				"Calendar connections");
	}

	@GetMapping("/api/v1/integrations/google/calendars")
	@Operation(summary = "List Google calendars for the connected account")
	@StandardApiErrors
	public ApiResponse<List<CalendarProvider.ExternalCalendar>> googleCalendars() {
		CalendarProvider provider = providers.stream().filter(item -> item.supports(com.rsvp.common.enums.CalendarProviderType.GOOGLE))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CALENDAR_NOT_FOUND, "Google calendar is not available"));
		return ApiResponse.of(provider.listCalendars(SecurityUtils.currentUserId()), "Google calendars");
	}

	@PutMapping("/api/v1/calendar/connections/{connectionId}")
	@Transactional
	@Operation(summary = "Select the calendar RSVP is allowed to write")
	@StandardApiErrors
	public ApiResponse<CalendarConnectionResponse> select(@PathVariable UUID connectionId,
			@Valid @RequestBody SelectCalendarRequest request) {
		CalendarConnection connection = calendarConnectionRepository.findByUuidAndUser_Id(connectionId, SecurityUtils.currentUserId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CALENDAR_NOT_FOUND, "Calendar connection not found"));
		connection.setExternalCalendarId(request.externalCalendarId());
		if (request.enabled() != null) {
			connection.setEnabled(request.enabled());
		}
		if (Boolean.TRUE.equals(request.isDefault())) {
			calendarConnectionRepository.findByUser_Id(connection.getUser().getId()).forEach(item -> item.setDefault(false));
			connection.setDefault(true);
		}
		return ApiResponse.of(toResponse(connection), "Calendar selected");
	}

	@PostMapping("/api/v1/calendar/sync")
	@Operation(summary = "Push RSVP events, travel, and tasks to connected calendars")
	@StandardApiErrors
	public ApiResponse<Integer> sync() {
		int count = calendarSyncService.syncUser(SecurityUtils.currentUserId());
		return ApiResponse.of(count, "Calendar sync requested");
	}

	@PostMapping("/api/v1/events/{eventId}/calendar/sync")
	@Operation(summary = "Sync one event and its travel and tasks")
	@StandardApiErrors
	public ApiResponse<Void> syncEvent(@PathVariable UUID eventId) {
		var event = eventService.require(SecurityUtils.currentUserId(), eventId);
		calendarSyncService.syncEventTree(event.getId());
		return ApiResponse.message("Event sync requested");
	}

	@GetMapping(value = "/api/v1/events/{eventId}/calendar.ics", produces = "text/calendar")
	@Operation(summary = "Download an event as iCalendar")
	@StandardApiErrors
	public ResponseEntity<String> eventIcs(@PathVariable UUID eventId) {
		return calendar(icsService.eventCalendar(SecurityUtils.currentUserId(), eventId));
	}

	@GetMapping(value = "/api/v1/travels/{travelId}/calendar.ics", produces = "text/calendar")
	@Operation(summary = "Download travel as iCalendar")
	@StandardApiErrors
	public ResponseEntity<String> travelIcs(@PathVariable UUID travelId) {
		travelService.require(SecurityUtils.currentUserId(), travelId);
		return calendar(icsService.travelCalendar(SecurityUtils.currentUserId(), travelId));
	}

	@GetMapping(value = "/api/v1/tasks/{taskId}/calendar.ics", produces = "text/calendar")
	@Operation(summary = "Download a task as iCalendar")
	@StandardApiErrors
	public ResponseEntity<String> taskIcs(@PathVariable UUID taskId) {
		taskService.require(SecurityUtils.currentUserId(), taskId);
		return calendar(icsService.taskCalendar(SecurityUtils.currentUserId(), taskId));
	}

	@GetMapping(value = "/api/v1/calendar/feeds/{token}.ics", produces = "text/calendar")
	@SecurityRequirements
	@Operation(summary = "Subscribe from Apple Calendar using a secret feed token")
	public ResponseEntity<String> feed(@PathVariable String token) {
		return calendar(icsService.feed(token));
	}

	private ResponseEntity<String> calendar(String body) {
		return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8")).body(body);
	}

	private CalendarConnectionResponse toResponse(CalendarConnection connection) {
		String subscription = connection.getFeedToken() == null ? null
				: properties.getPublicBaseUrl() + "/api/v1/calendar/feeds/" + connection.getFeedToken() + ".ics";
		return new CalendarConnectionResponse(connection.getUuid(), connection.getProvider(), connection.getExternalCalendarId(),
				connection.getCalendarName(), connection.isDefault(), connection.isEnabled(), connection.getSyncStatus(),
				connection.getLastSyncedAt(), subscription);
	}
}
