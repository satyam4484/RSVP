package com.rsvp.event.repository;

import com.rsvp.common.enums.EventStatus;
import com.rsvp.event.entity.Event;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class EventSpecifications {

	private EventSpecifications() {
	}

	public static Specification<Event> filter(Long userId, EventStatus status, Instant dateFrom, Instant dateTo,
			String search) {
		return (root, query, cb) -> {
			var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
			predicates.add(cb.equal(root.get("user").get("id"), userId));
			predicates.add(cb.isFalse(root.get("deleted")));
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (dateFrom != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("startDateTime"), dateFrom));
			}
			if (dateTo != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("startDateTime"), dateTo));
			}
			if (search != null && !search.isBlank()) {
				String like = "%" + search.trim().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("name")), like),
						cb.like(cb.lower(root.get("description")), like),
						cb.like(cb.lower(root.get("locationName")), like)));
			}
			return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}
}
