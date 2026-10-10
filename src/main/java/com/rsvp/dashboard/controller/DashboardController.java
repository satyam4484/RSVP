package com.rsvp.dashboard.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.dashboard.dto.DashboardResponse;
import com.rsvp.dashboard.dto.EventSummaryResponse;
import com.rsvp.dashboard.dto.TimelineItem;
import com.rsvp.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/api/v1/dashboard")
	@Operation(summary = "Upcoming events, tasks, travel, and expense totals")
	@StandardApiErrors
	public ApiResponse<DashboardResponse> dashboard() {
		return ApiResponse.of(dashboardService.dashboard(SecurityUtils.currentUserId()), "Dashboard");
	}

	@GetMapping("/api/v1/events/{eventId}/summary")
	@Operation(summary = "Combined summary for one event")
	@StandardApiErrors
	public ApiResponse<EventSummaryResponse> summary(@PathVariable UUID eventId) {
		return ApiResponse.of(dashboardService.summary(SecurityUtils.currentUserId(), eventId), "Event summary");
	}

	@GetMapping("/api/v1/events/{eventId}/timeline")
	@Operation(summary = "Chronological timeline of travel, tasks, expenses, payments, and reminders")
	@StandardApiErrors
	public ApiResponse<List<TimelineItem>> timeline(@PathVariable UUID eventId) {
		return ApiResponse.of(dashboardService.timeline(SecurityUtils.currentUserId(), eventId), "Timeline");
	}
}
