package com.rsvp.search.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.search.dto.SearchResponse;
import com.rsvp.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search")
public class SearchController {

	private final SearchService searchService;

	@GetMapping
	@Operation(summary = "Search events, contacts, vendors, travel, tasks, and documents")
	@StandardApiErrors
	public ApiResponse<SearchResponse> search(@RequestParam("q") String query) {
		return ApiResponse.of(searchService.search(SecurityUtils.currentUserId(), query), "Search results");
	}
}
