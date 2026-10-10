package com.rsvp.event.service;

import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class EventLifecycle {

	private static final Map<EventStatus, Set<EventStatus>> ALLOWED = Map.of(
			EventStatus.PLANNING, EnumSet.of(EventStatus.CONFIRMED, EventStatus.CANCELLED, EventStatus.COMPLETED),
			EventStatus.CONFIRMED, EnumSet.of(EventStatus.IN_PROGRESS, EventStatus.CANCELLED, EventStatus.COMPLETED),
			EventStatus.IN_PROGRESS, EnumSet.of(EventStatus.COMPLETED),
			EventStatus.COMPLETED, EnumSet.of(EventStatus.ARCHIVED),
			EventStatus.CANCELLED, EnumSet.of(EventStatus.ARCHIVED),
			EventStatus.ARCHIVED, EnumSet.noneOf(EventStatus.class));

	private EventLifecycle() {
	}

	public static void assertTransition(EventStatus from, EventStatus to) {
		if (from == to) {
			return;
		}
		if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
			throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
					"Cannot change event status from " + from + " to " + to);
		}
	}

	public static boolean autoCompletable(EventStatus status) {
		return status == EventStatus.CONFIRMED || status == EventStatus.IN_PROGRESS;
	}
}
