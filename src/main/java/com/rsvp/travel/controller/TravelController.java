package com.rsvp.travel.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.response.PagedResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.travel.dto.TravelRequest;
import com.rsvp.travel.dto.TravelResponse;
import com.rsvp.travel.service.TravelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Travel")
public class TravelController {

	private final TravelService travelService;

	@PostMapping("/api/v1/events/{eventId}/travels")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Add travel of any type to an event")
	@StandardApiErrors
	public ApiResponse<TravelResponse> create(@PathVariable UUID eventId, @Valid @RequestBody TravelRequest request) {
		return ApiResponse.of(travelService.create(SecurityUtils.currentUserId(), eventId, request), "Travel created");
	}

	@GetMapping("/api/v1/events/{eventId}/travels")
	@Operation(summary = "List travel for an event")
	@StandardApiErrors
	public PagedResponse<TravelResponse> list(
			@PathVariable UUID eventId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "departureAt,asc") String sort) {
		return PagedResponse.from(travelService.list(SecurityUtils.currentUserId(), eventId, page, size, sort));
	}

	@GetMapping("/api/v1/travels/{travelId}")
	@Operation(summary = "Get a travel record")
	@StandardApiErrors
	public ApiResponse<TravelResponse> get(@PathVariable UUID travelId) {
		return ApiResponse.of(travelService.get(SecurityUtils.currentUserId(), travelId), "Travel");
	}

	@PutMapping("/api/v1/travels/{travelId}")
	@Operation(summary = "Update a travel record")
	@StandardApiErrors
	public ApiResponse<TravelResponse> update(@PathVariable UUID travelId, @Valid @RequestBody TravelRequest request) {
		return ApiResponse.of(travelService.update(SecurityUtils.currentUserId(), travelId, request), "Travel updated");
	}

	@DeleteMapping("/api/v1/travels/{travelId}")
	@Operation(summary = "Soft-delete a travel record")
	@StandardApiErrors
	public ApiResponse<Void> delete(@PathVariable UUID travelId) {
		travelService.delete(SecurityUtils.currentUserId(), travelId);
		return ApiResponse.message("Travel deleted");
	}
}
