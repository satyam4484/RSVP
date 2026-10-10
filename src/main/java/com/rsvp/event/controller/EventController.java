package com.rsvp.event.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.response.PagedResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.event.dto.EventRequest;
import com.rsvp.event.dto.EventResponse;
import com.rsvp.event.dto.EventStatusRequest;
import com.rsvp.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events")
public class EventController {

	private final EventService eventService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an event")
	@StandardApiErrors
	public ApiResponse<EventResponse> create(@Valid @RequestBody EventRequest request) {
		return ApiResponse.of(eventService.create(SecurityUtils.currentUserId(), request), "Event created successfully");
	}

	@GetMapping
	@Operation(summary = "List events for the current user")
	@StandardApiErrors
	public PagedResponse<EventResponse> list(
			@RequestParam(required = false) EventStatus status,
			@RequestParam(required = false) Instant dateFrom,
			@RequestParam(required = false) Instant dateTo,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "startDateTime,asc") String sort) {
		return PagedResponse.from(eventService.list(SecurityUtils.currentUserId(), status, dateFrom, dateTo, search, page,
				size, sort));
	}

	@GetMapping("/{eventId}")
	@Operation(summary = "Get an event")
	@StandardApiErrors
	public ApiResponse<EventResponse> get(@PathVariable UUID eventId) {
		return ApiResponse.of(eventService.get(SecurityUtils.currentUserId(), eventId), "Event");
	}

	@PutMapping("/{eventId}")
	@Operation(summary = "Update an event")
	@StandardApiErrors
	public ApiResponse<EventResponse> update(@PathVariable UUID eventId, @Valid @RequestBody EventRequest request) {
		return ApiResponse.of(eventService.update(SecurityUtils.currentUserId(), eventId, request), "Event updated");
	}

	@PatchMapping("/{eventId}/status")
	@Operation(summary = "Change event status using the lifecycle rules")
	@StandardApiErrors
	public ApiResponse<EventResponse> status(@PathVariable UUID eventId, @Valid @RequestBody EventStatusRequest request) {
		return ApiResponse.of(eventService.changeStatus(SecurityUtils.currentUserId(), eventId, request.status()),
				"Event status updated");
	}

	@PostMapping("/{eventId}/complete")
	@Operation(summary = "Manually complete an event")
	@StandardApiErrors
	public ApiResponse<EventResponse> complete(@PathVariable UUID eventId) {
		return ApiResponse.of(eventService.complete(SecurityUtils.currentUserId(), eventId), "Event completed");
	}

	@PostMapping("/{eventId}/archive")
	@Operation(summary = "Archive a completed or cancelled event")
	@StandardApiErrors
	public ApiResponse<EventResponse> archive(@PathVariable UUID eventId) {
		return ApiResponse.of(eventService.archive(SecurityUtils.currentUserId(), eventId), "Event archived");
	}

	@DeleteMapping("/{eventId}")
	@Operation(summary = "Soft-delete an event")
	@StandardApiErrors
	public ApiResponse<Void> delete(@PathVariable UUID eventId) {
		eventService.delete(SecurityUtils.currentUserId(), eventId);
		return ApiResponse.message("Event deleted");
	}
}
