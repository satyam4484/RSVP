package com.rsvp.search.dto;

import java.util.List;
import java.util.UUID;

public record SearchResponse(
		List<SearchHit> events,
		List<SearchHit> contacts,
		List<SearchHit> vendors,
		List<SearchHit> tasks,
		List<SearchHit> travel,
		List<SearchHit> documents) {

	public record SearchHit(UUID id, String title, String subtitle) {
	}
}
