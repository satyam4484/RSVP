package com.rsvp.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.calendar.entity.CalendarEventMapping;
import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.calendar.repository.CalendarEventMappingRepository;
import com.rsvp.calendar.service.CalendarProvider;
import com.rsvp.calendar.service.CalendarProvider.CalendarEventRequest;
import com.rsvp.calendar.service.CalendarProvider.ExternalCalendarEvent;
import com.rsvp.calendar.service.CalendarSyncService;
import com.rsvp.common.enums.CalendarProviderType;
import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.enums.SyncAction;
import com.rsvp.common.enums.SyncStatus;
import com.rsvp.common.enums.SyncSubject;
import com.rsvp.common.event.CalendarSyncRequested;
import com.rsvp.event.entity.Event;
import com.rsvp.event.repository.EventRepository;
import com.rsvp.task.repository.TaskRepository;
import com.rsvp.travel.repository.TravelRepository;
import com.rsvp.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarSyncServiceTest {

	@Mock
	private EventRepository eventRepository;
	@Mock
	private TravelRepository travelRepository;
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private CalendarConnectionRepository connectionRepository;
	@Mock
	private CalendarEventMappingRepository mappingRepository;
	@Mock
	private CalendarProvider provider;

	private CalendarSyncService syncService;
	private Event event;
	private CalendarConnection connection;

	@BeforeEach
	void setUp() {
		lenient().when(provider.supports(CalendarProviderType.GOOGLE)).thenReturn(true);
		syncService = new CalendarSyncService(eventRepository, travelRepository, taskRepository, connectionRepository,
				mappingRepository, List.of(provider));
		User user = new User();
		user.setId(1L);
		event = new Event();
		event.setId(4L);
		event.setUuid(UUID.randomUUID());
		event.setUser(user);
		event.setName("Tech Conference");
		event.setStatus(EventStatus.CONFIRMED);
		event.setStartDateTime(Instant.parse("2026-10-20T02:00:00Z"));
		event.setEndDateTime(Instant.parse("2026-10-20T10:00:00Z"));
		event.setTimezone("Asia/Singapore");
		connection = new CalendarConnection();
		connection.setId(8L);
		connection.setProvider(CalendarProviderType.GOOGLE);
		connection.setEnabled(true);
	}

	@Test
	void createsThenUpdatesWithoutASecondRemoteCreate() {
		when(eventRepository.findById(4L)).thenReturn(Optional.of(event));
		when(connectionRepository.findByUser_IdAndEnabledTrue(1L)).thenReturn(List.of(connection));
		when(mappingRepository.findByCalendarConnection_IdAndSubjectTypeAndSubjectId(8L, SyncSubject.EVENT, 4L))
				.thenReturn(Optional.empty());
		when(mappingRepository.save(any(CalendarEventMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(provider.upsert(eq(8L), any())).thenReturn(new ExternalCalendarEvent("google-1", "https://calendar.example/1"));

		syncService.synchronize(new CalendarSyncRequested(SyncSubject.EVENT, 4L, SyncAction.UPSERT));

		CalendarEventMapping mapping = new CalendarEventMapping();
		mapping.setExternalEventId("google-1");
		mapping.setSyncStatus(SyncStatus.SYNCED);
		mapping.setAttemptCount(1);
		when(mappingRepository.findByCalendarConnection_IdAndSubjectTypeAndSubjectId(8L, SyncSubject.EVENT, 4L))
				.thenReturn(Optional.of(mapping));
		syncService.synchronize(new CalendarSyncRequested(SyncSubject.EVENT, 4L, SyncAction.UPSERT));

		ArgumentCaptor<CalendarEventRequest> captor = ArgumentCaptor.forClass(CalendarEventRequest.class);
		verify(provider, org.mockito.Mockito.times(2)).upsert(eq(8L), captor.capture());
		assertThat(captor.getAllValues().get(1).externalEventId()).isEqualTo("google-1");
	}

	@Test
	void marksAFailedSyncAndRetriesIt() {
		when(eventRepository.findById(4L)).thenReturn(Optional.of(event));
		when(connectionRepository.findByUser_IdAndEnabledTrue(1L)).thenReturn(List.of(connection));
		CalendarEventMapping mapping = new CalendarEventMapping();
		mapping.setSubjectType(SyncSubject.EVENT);
		mapping.setSubjectId(4L);
		mapping.setSyncStatus(SyncStatus.PENDING);
		mapping.setAttemptCount(0);
		mapping.setUpdatedAt(Instant.parse("2020-01-01T00:00:00Z"));
		when(mappingRepository.findByCalendarConnection_IdAndSubjectTypeAndSubjectId(8L, SyncSubject.EVENT, 4L))
				.thenReturn(Optional.of(mapping));
		when(provider.upsert(eq(8L), any())).thenThrow(new RuntimeException("Google unavailable")).thenReturn(
				new ExternalCalendarEvent("google-1", null));

		syncService.synchronize(new CalendarSyncRequested(SyncSubject.EVENT, 4L, SyncAction.UPSERT));
		assertThat(mapping.getSyncStatus()).isEqualTo(SyncStatus.FAILED);

		when(mappingRepository.findBySyncStatusIn(any())).thenReturn(List.of(mapping));
		syncService.retryFailed();
		assertThat(mapping.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
		assertThat(mapping.getExternalEventId()).isEqualTo("google-1");
	}

	@Test
	void deletesOnceAndIgnoresASecondDelete() {
		event.setDeleted(true);
		when(eventRepository.findById(4L)).thenReturn(Optional.of(event));
		when(connectionRepository.findByUser_IdAndEnabledTrue(1L)).thenReturn(List.of(connection));
		CalendarEventMapping mapping = new CalendarEventMapping();
		mapping.setExternalEventId("google-1");
		mapping.setSyncStatus(SyncStatus.SYNCED);
		mapping.setAttemptCount(1);
		when(mappingRepository.findByCalendarConnection_IdAndSubjectTypeAndSubjectId(8L, SyncSubject.EVENT, 4L))
				.thenReturn(Optional.of(mapping));
		syncService.synchronize(new CalendarSyncRequested(SyncSubject.EVENT, 4L, SyncAction.DELETE));
		verify(provider).delete(8L, "google-1", "event-" + event.getUuid());
		assertThat(mapping.getSyncStatus()).isEqualTo(SyncStatus.DELETED);

		syncService.synchronize(new CalendarSyncRequested(SyncSubject.EVENT, 4L, SyncAction.DELETE));
		verify(provider, org.mockito.Mockito.times(1)).delete(any(), any(), any());
	}

	@Test
	void doesNotCallTheProviderWhenNoCalendarIsConnected() {
		when(eventRepository.findById(4L)).thenReturn(Optional.of(event));
		when(connectionRepository.findByUser_IdAndEnabledTrue(1L)).thenReturn(List.of());
		syncService.synchronize(new CalendarSyncRequested(SyncSubject.EVENT, 4L, SyncAction.UPSERT));
		verify(provider, never()).upsert(any(), any());
	}
}
