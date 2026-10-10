package com.rsvp.calendar.service;

import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.calendar.entity.CalendarEventMapping;
import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.calendar.repository.CalendarEventMappingRepository;
import com.rsvp.calendar.service.CalendarProvider.CalendarEventRequest;
import com.rsvp.calendar.service.CalendarProvider.ExternalCalendarEvent;
import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.enums.SyncAction;
import com.rsvp.common.enums.SyncStatus;
import com.rsvp.common.enums.SyncSubject;
import com.rsvp.common.event.CalendarSyncRequested;
import com.rsvp.event.entity.Event;
import com.rsvp.event.repository.EventRepository;
import com.rsvp.task.entity.Task;
import com.rsvp.task.repository.TaskRepository;
import com.rsvp.travel.entity.Travel;
import com.rsvp.travel.repository.TravelRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarSyncService {

	private static final Logger log = LoggerFactory.getLogger(CalendarSyncService.class);

	private final EventRepository eventRepository;
	private final TravelRepository travelRepository;
	private final TaskRepository taskRepository;
	private final CalendarConnectionRepository calendarConnectionRepository;
	private final CalendarEventMappingRepository mappingRepository;
	private final List<CalendarProvider> providers;

	@Transactional
	public void synchronize(CalendarSyncRequested request) {
		Snapshot snapshot = load(request.subject(), request.subjectId());
		if (snapshot == null || snapshot.skip()) {
			return;
		}
		SyncAction action = snapshot.deleted() ? SyncAction.DELETE : request.action();
		for (CalendarConnection connection : calendarConnectionRepository.findByUser_IdAndEnabledTrue(snapshot.userId())) {
			syncOne(connection, snapshot, action);
		}
	}

	@Transactional
	public void retryFailed() {
		for (CalendarEventMapping mapping : mappingRepository.findBySyncStatusIn(List.of(SyncStatus.FAILED, SyncStatus.PENDING))) {
			if (mapping.getUpdatedAt() != null && mapping.getUpdatedAt().isAfter(Instant.now().minusSeconds(45))) {
				continue;
			}
			synchronize(new CalendarSyncRequested(mapping.getSubjectType(), mapping.getSubjectId(),
					mapping.getSyncStatus() == SyncStatus.DELETED ? SyncAction.DELETE : SyncAction.UPSERT));
		}
	}

	@Transactional
	public void syncEventTree(Long eventId) {
		synchronize(new CalendarSyncRequested(SyncSubject.EVENT, eventId, SyncAction.UPSERT));
		travelRepository.findByEvent_IdAndDeletedFalse(eventId)
				.forEach(travel -> synchronize(new CalendarSyncRequested(SyncSubject.TRAVEL, travel.getId(), SyncAction.UPSERT)));
		taskRepository.findByEvent_IdAndDeletedFalse(eventId)
				.forEach(task -> synchronize(new CalendarSyncRequested(SyncSubject.TASK, task.getId(), SyncAction.UPSERT)));
	}

	@Transactional
	public int syncUser(Long userId) {
		int count = 0;
		for (Event event : eventRepository.findByUser_IdAndDeletedFalse(userId)) {
			synchronize(new CalendarSyncRequested(SyncSubject.EVENT, event.getId(), SyncAction.UPSERT));
			count++;
		}
		for (Travel travel : travelRepository.findByEvent_User_IdAndDeletedFalse(userId)) {
			synchronize(new CalendarSyncRequested(SyncSubject.TRAVEL, travel.getId(), SyncAction.UPSERT));
			count++;
		}
		for (Task task : taskRepository.findByEvent_User_IdAndDeletedFalse(userId)) {
			synchronize(new CalendarSyncRequested(SyncSubject.TASK, task.getId(), SyncAction.UPSERT));
			count++;
		}
		return count;
	}

	private void syncOne(CalendarConnection connection, Snapshot snapshot, SyncAction action) {
		try {
			CalendarProvider provider = provider(connection);
			CalendarEventMapping mapping = mappingRepository
					.findByCalendarConnection_IdAndSubjectTypeAndSubjectId(connection.getId(), snapshot.subject(), snapshot.subjectId())
					.orElseGet(() -> newMapping(connection, snapshot));
			if (action == SyncAction.DELETE) {
				if (mapping.getSyncStatus() != SyncStatus.DELETED) {
					provider.delete(connection.getId(), mapping.getExternalEventId(), snapshot.idempotencyKey());
					mapping.setSyncStatus(SyncStatus.DELETED);
					mapping.setLastSyncedAt(Instant.now());
					mapping.setSyncError(null);
					mapping.setAttemptCount(mapping.getAttemptCount() + 1);
					mappingRepository.save(mapping);
				}
			} else {
				CalendarEventRequest request = snapshot.request(mapping.getExternalEventId());
				ExternalCalendarEvent external = provider.upsert(connection.getId(), request);
				mapping.setExternalEventId(external.id());
				mapping.setExternalEventUrl(external.url());
				mapping.setSyncStatus(SyncStatus.SYNCED);
				mapping.setSyncError(null);
				mapping.setLastSyncedAt(Instant.now());
				mapping.setAttemptCount(mapping.getAttemptCount() + 1);
				mappingRepository.save(mapping);
			}
			connection.setSyncStatus(SyncStatus.SYNCED);
			connection.setLastSyncedAt(Instant.now());
			log.info("operation=calendar-sync provider={} success=true subject={} subjectId={} durationMs=0",
					connection.getProvider(), snapshot.subject(), snapshot.subjectId());
		} catch (RuntimeException ex) {
			markFailed(connection, snapshot, ex);
		}
	}

	private void markFailed(CalendarConnection connection, Snapshot snapshot, RuntimeException ex) {
		CalendarEventMapping mapping = mappingRepository
				.findByCalendarConnection_IdAndSubjectTypeAndSubjectId(connection.getId(), snapshot.subject(), snapshot.subjectId())
				.orElseGet(() -> newMapping(connection, snapshot));
		mapping.setSyncStatus(SyncStatus.FAILED);
		mapping.setSyncError(trim(ex.getMessage()));
		mapping.setAttemptCount(mapping.getAttemptCount() + 1);
		mappingRepository.save(mapping);
		connection.setSyncStatus(SyncStatus.FAILED);
		log.warn("operation=calendar-sync provider={} success=false subject={} subjectId={}", connection.getProvider(),
				snapshot.subject(), snapshot.subjectId());
	}

	private CalendarEventMapping newMapping(CalendarConnection connection, Snapshot snapshot) {
		CalendarEventMapping mapping = new CalendarEventMapping();
		mapping.setCalendarConnection(connection);
		mapping.setSubjectType(snapshot.subject());
		mapping.setSubjectId(snapshot.subjectId());
		mapping.setEvent(snapshot.event());
		mapping.setTravel(snapshot.travel());
		mapping.setTask(snapshot.task());
		mapping.setIdempotencyKey(snapshot.idempotencyKey());
		mapping.setSyncStatus(SyncStatus.PENDING);
		mapping.setAttemptCount(0);
		return mappingRepository.save(mapping);
	}

	private CalendarProvider provider(CalendarConnection connection) {
		return providers.stream().filter(provider -> provider.supports(connection.getProvider())).findFirst()
				.orElseThrow(() -> new IllegalStateException("No calendar provider for " + connection.getProvider()));
	}

	private Snapshot load(SyncSubject subject, Long subjectId) {
		return switch (subject) {
			case EVENT -> eventRepository.findById(subjectId).map(this::fromEvent).orElse(null);
			case TRAVEL -> travelRepository.findById(subjectId).map(this::fromTravel).orElse(null);
			case TASK -> taskRepository.findById(subjectId).map(this::fromTask).orElse(null);
		};
	}

	private Snapshot fromEvent(Event event) {
		return new Snapshot(event.getUser().getId(), SyncSubject.EVENT, event.getId(), event.isDeleted(), false,
				"event-" + event.getUuid(), event.getName(), event.getDescription(), location(event.getLocationName(), event.getAddress()),
				event.getStartDateTime(), event.getEndDateTime(), event.getTimezone(),
				event.getStatus() == EventStatus.CANCELLED ? "cancelled" : "confirmed", event, null, null);
	}

	private Snapshot fromTravel(Travel travel) {
		boolean skip = travel.getDepartureAt() == null;
		Instant end = travel.getArrivalAt() != null ? travel.getArrivalAt()
				: travel.getDepartureAt() == null ? null : travel.getDepartureAt().plus(1, ChronoUnit.HOURS);
		String title = travel.getTitle();
		if (travel.getFromLocation() != null && travel.getToLocation() != null) {
			title = travel.getFromLocation() + " → " + travel.getToLocation();
		}
		return new Snapshot(travel.getEvent().getUser().getId(), SyncSubject.TRAVEL, travel.getId(), travel.isDeleted(), skip,
				"travel-" + travel.getUuid(), title, travel.getNotes(), travel.getFromLocation(), travel.getDepartureAt(), end,
				travel.getTimezone(), "confirmed", travel.getEvent(), travel, null);
	}

	private Snapshot fromTask(Task task) {
		boolean skip = task.getDueAt() == null;
		Instant end = task.getDueAt() == null ? null : task.getDueAt().plus(30, ChronoUnit.MINUTES);
		return new Snapshot(task.getEvent().getUser().getId(), SyncSubject.TASK, task.getId(), task.isDeleted(), skip,
				"task-" + task.getUuid(), task.getTitle(), task.getDescription(), task.getEvent().getLocationName(), task.getDueAt(),
				end, task.getEvent().getTimezone(), task.getStatus().name().equals("CANCELLED") ? "cancelled" : "confirmed",
				task.getEvent(), null, task);
	}

	private String location(String name, String address) {
		if (name == null || name.isBlank()) {
			return address;
		}
		if (address == null || address.isBlank()) {
			return name;
		}
		return name + ", " + address;
	}

	private String trim(String message) {
		if (message == null) {
			return "Calendar sync failed";
		}
		return message.length() > 500 ? message.substring(0, 500) : message;
	}

	private record Snapshot(Long userId, SyncSubject subject, Long subjectId, boolean deleted, boolean skip, String idempotencyKey,
			String summary, String description, String location, Instant start, Instant end, String timezone, String status, Event event,
			Travel travel, Task task) {

		CalendarEventRequest request(String externalEventId) {
			return new CalendarEventRequest(idempotencyKey, externalEventId, summary, description, location, start, end, timezone, status);
		}
	}
}
