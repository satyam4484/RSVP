package com.rsvp.event.service;

import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.event.CalendarSyncRequested;
import com.rsvp.common.enums.SyncAction;
import com.rsvp.common.enums.SyncSubject;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.common.util.PageRequests;
import com.rsvp.common.util.Timezones;
import com.rsvp.event.dto.EventRequest;
import com.rsvp.event.dto.EventResponse;
import com.rsvp.event.entity.Event;
import com.rsvp.event.mapper.EventMapper;
import com.rsvp.event.repository.EventRepository;
import com.rsvp.event.repository.EventSpecifications;
import com.rsvp.user.entity.User;
import com.rsvp.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

	private static final Set<String> SORTS = Set.of("startDateTime", "endDateTime", "name", "createdAt", "status");

	private final EventRepository eventRepository;
	private final UserService userService;
	private final EventMapper eventMapper;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	@Transactional
	public EventResponse create(Long userId, EventRequest request) {
		User user = userService.requireActive(userId);
		validate(request);
		Event event = new Event();
		event.setUser(user);
		event.setStatus(EventStatus.PLANNING);
		eventMapper.apply(request, event, Timezones.require(request.timezone()).getId());
		Event saved = eventRepository.save(event);
		publish(saved.getId(), SyncAction.UPSERT);
		return eventMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public Page<EventResponse> list(Long userId, EventStatus status, Instant dateFrom, Instant dateTo, String search,
			int page, int size, String sort) {
		Pageable pageable = PageRequests.of(page, size, sort, SORTS, "startDateTime,asc");
		return eventRepository.findAll(EventSpecifications.filter(userId, status, dateFrom, dateTo, search), pageable)
				.map(eventMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public EventResponse get(Long userId, UUID eventId) {
		return eventMapper.toResponse(require(userId, eventId));
	}

	@Transactional
	public EventResponse update(Long userId, UUID eventId, EventRequest request) {
		Event event = require(userId, eventId);
		validate(request);
		eventMapper.apply(request, event, Timezones.require(request.timezone()).getId());
		publish(event.getId(), SyncAction.UPSERT);
		return eventMapper.toResponse(event);
	}

	@Transactional
	public EventResponse changeStatus(Long userId, UUID eventId, EventStatus status) {
		Event event = require(userId, eventId);
		applyStatus(event, status);
		publish(event.getId(), SyncAction.UPSERT);
		return eventMapper.toResponse(event);
	}

	@Transactional
	public EventResponse complete(Long userId, UUID eventId) {
		return changeStatus(userId, eventId, EventStatus.COMPLETED);
	}

	@Transactional
	public EventResponse archive(Long userId, UUID eventId) {
		return changeStatus(userId, eventId, EventStatus.ARCHIVED);
	}

	@Transactional
	public void delete(Long userId, UUID eventId) {
		Event event = require(userId, eventId);
		event.markDeleted(clock.instant());
		publish(event.getId(), SyncAction.DELETE);
	}

	@Transactional
	public int autoCompleteDueEvents() {
		Instant now = clock.instant();
		List<Event> due = eventRepository.findByStatusInAndEndDateTimeBeforeAndDeletedFalse(
				List.of(EventStatus.CONFIRMED, EventStatus.IN_PROGRESS), now);
		int completed = 0;
		for (Event event : due) {
			if (!EventLifecycle.autoCompletable(event.getStatus())) {
				continue;
			}
			applyStatus(event, EventStatus.COMPLETED);
			publish(event.getId(), SyncAction.UPSERT);
			completed++;
		}
		return completed;
	}

	@Transactional(readOnly = true)
	public Event require(Long userId, UUID eventId) {
		return eventRepository.findByUuidAndUser_IdAndDeletedFalse(eventId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EVENT_NOT_FOUND, "Event not found"));
	}

	public EventResponse toResponse(Event event) {
		return eventMapper.toResponse(event);
	}

	private void applyStatus(Event event, EventStatus status) {
		EventLifecycle.assertTransition(event.getStatus(), status);
		if (event.getStatus() == status) {
			return;
		}
		event.setStatus(status);
		if (status == EventStatus.COMPLETED && event.getCompletedAt() == null) {
			event.setCompletedAt(clock.instant());
		}
		if (status == EventStatus.ARCHIVED && event.getArchivedAt() == null) {
			event.setArchivedAt(clock.instant());
		}
	}

	private void validate(EventRequest request) {
		if (!request.endDateTime().isAfter(request.startDateTime())) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "Event end must be after the start");
		}
		Timezones.require(request.timezone());
		boolean hasLatitude = request.latitude() != null;
		boolean hasLongitude = request.longitude() != null;
		if (hasLatitude != hasLongitude) {
			throw new BusinessException(ErrorCode.INVALID_COORDINATES, "Latitude and longitude must be provided together");
		}
		if (hasLatitude) {
			if (request.latitude().compareTo(BigDecimal.valueOf(-90)) < 0
					|| request.latitude().compareTo(BigDecimal.valueOf(90)) > 0
					|| request.longitude().compareTo(BigDecimal.valueOf(-180)) < 0
					|| request.longitude().compareTo(BigDecimal.valueOf(180)) > 0) {
				throw new BusinessException(ErrorCode.INVALID_COORDINATES, "Coordinates are out of range");
			}
		}
	}

	private void publish(Long eventId, SyncAction action) {
		eventPublisher.publishEvent(new CalendarSyncRequested(SyncSubject.EVENT, eventId, action));
	}
}
