package com.rsvp.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.enums.SyncAction;
import com.rsvp.common.event.CalendarSyncRequested;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.event.dto.EventRequest;
import com.rsvp.event.entity.Event;
import com.rsvp.event.mapper.EventMapper;
import com.rsvp.event.repository.EventRepository;
import com.rsvp.event.service.EventService;
import com.rsvp.user.entity.User;
import com.rsvp.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	@Mock
	private EventRepository eventRepository;
	@Mock
	private UserService userService;
	@Mock
	private ApplicationEventPublisher publisher;

	private EventService eventService;
	private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);

	@BeforeEach
	void setUp() {
		eventService = new EventService(eventRepository, userService, new EventMapper(), publisher, clock);
	}

	@Test
	void createsUpdatesDeletesAndCompletesAnEvent() {
		User user = user(1L);
		when(userService.requireActive(1L)).thenReturn(user);
		when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
			Event event = invocation.getArgument(0);
			event.setId(5L);
			event.setUuid(UUID.randomUUID());
			return event;
		});
		var created = eventService.create(1L, request());
		assertThat(created.status()).isEqualTo(EventStatus.PLANNING);
		verify(publisher).publishEvent(any(CalendarSyncRequested.class));

		Event stored = savedEvent(user, EventStatus.PLANNING);
		when(eventRepository.findByUuidAndUser_IdAndDeletedFalse(stored.getUuid(), 1L)).thenReturn(Optional.of(stored));
		var updated = eventService.update(1L, stored.getUuid(), request());
		assertThat(updated.name()).isEqualTo("Tech Conference");

		eventService.complete(1L, stored.getUuid());
		assertThat(stored.getStatus()).isEqualTo(EventStatus.COMPLETED);
		assertThat(stored.getCompletedAt()).isEqualTo(clock.instant());

		stored.setStatus(EventStatus.COMPLETED);
		eventService.delete(1L, stored.getUuid());
		assertThat(stored.isDeleted()).isTrue();
	}

	@Test
	void cancelsFromPlanningAndRejectsAnInvalidTransition() {
		User user = user(1L);
		Event stored = savedEvent(user, EventStatus.PLANNING);
		when(eventRepository.findByUuidAndUser_IdAndDeletedFalse(stored.getUuid(), 1L)).thenReturn(Optional.of(stored));
		eventService.changeStatus(1L, stored.getUuid(), EventStatus.CANCELLED);
		assertThat(stored.getStatus()).isEqualTo(EventStatus.CANCELLED);

		stored.setStatus(EventStatus.COMPLETED);
		assertThatThrownBy(() -> eventService.changeStatus(1L, stored.getUuid(), EventStatus.PLANNING))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void autoCompletesConfirmedAndInProgressButNotPlanningOrCancelled() {
		Event confirmed = savedEvent(user(1L), EventStatus.CONFIRMED);
		Event inProgress = savedEvent(user(1L), EventStatus.IN_PROGRESS);
		Event planning = savedEvent(user(1L), EventStatus.PLANNING);
		when(eventRepository.findByStatusInAndEndDateTimeBeforeAndDeletedFalse(any(), any()))
				.thenReturn(List.of(confirmed, inProgress, planning));
		int completed = eventService.autoCompleteDueEvents();
		assertThat(completed).isEqualTo(2);
		assertThat(confirmed.getStatus()).isEqualTo(EventStatus.COMPLETED);
		assertThat(inProgress.getStatus()).isEqualTo(EventStatus.COMPLETED);
		assertThat(planning.getStatus()).isEqualTo(EventStatus.PLANNING);
	}

	private EventRequest request() {
		return new EventRequest("Tech Conference", "Annual", Instant.parse("2026-10-20T02:00:00Z"),
				Instant.parse("2026-10-20T10:00:00Z"), "Asia/Singapore", "Bangalore", null, null, null, "#336699", null);
	}

	private User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setUuid(UUID.randomUUID());
		user.setActive(true);
		return user;
	}

	private Event savedEvent(User user, EventStatus status) {
		Event event = new Event();
		event.setId(8L);
		event.setUuid(UUID.randomUUID());
		event.setUser(user);
		event.setStatus(status);
		event.setName("Tech Conference");
		event.setStartDateTime(Instant.parse("2026-10-01T02:00:00Z"));
		event.setEndDateTime(Instant.parse("2026-10-01T10:00:00Z"));
		event.setTimezone("Asia/Singapore");
		return event;
	}
}
