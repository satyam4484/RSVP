package com.rsvp.calendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.calendar.entity.ExternalAccountConnection;
import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.calendar.service.CalendarProvider.CalendarEventRequest;
import com.rsvp.calendar.service.CalendarProvider.ExternalCalendarEvent;
import com.rsvp.integration.google.GoogleAccountService;
import com.rsvp.integration.google.GoogleApiClient;
import com.rsvp.integration.google.calendar.GoogleCalendarProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarProviderTest {

	@Mock
	private GoogleApiClient googleApiClient;
	@Mock
	private GoogleAccountService googleAccountService;
	@Mock
	private CalendarConnectionRepository connectionRepository;

	private GoogleCalendarProvider provider;

	@BeforeEach
	void setUp() {
		provider = new GoogleCalendarProvider(googleApiClient, googleAccountService, connectionRepository);
		CalendarConnection connection = new CalendarConnection();
		connection.setId(3L);
		connection.setExternalCalendarId("cal-1");
		ExternalAccountConnection account = new ExternalAccountConnection();
		connection.setExternalAccount(account);
		when(connectionRepository.findById(3L)).thenReturn(Optional.of(connection));
		when(googleAccountService.accessToken(account)).thenReturn("token");
	}

	@Test
	void updatesAnExistingRemoteEventInsteadOfCreatingADuplicate() {
		when(googleApiClient.findByKey("token", "cal-1", "event-1"))
				.thenReturn(List.of(new ExternalCalendarEvent("remote-1", "https://example/1"),
						new ExternalCalendarEvent("remote-2", null)));
		when(googleApiClient.updateEvent(eq("token"), eq("cal-1"), any())).thenReturn(new ExternalCalendarEvent("remote-1", "https://example/1"));
		provider.upsert(3L, request(null));
		verify(googleApiClient, never()).createEvent(any(), any(), any());
		verify(googleApiClient).deleteEvent("token", "cal-1", "remote-2");
	}

	@Test
	void createsWhenNoRemoteCopyExists() {
		when(googleApiClient.findByKey("token", "cal-1", "event-1")).thenReturn(List.of());
		when(googleApiClient.createEvent(eq("token"), eq("cal-1"), any())).thenReturn(new ExternalCalendarEvent("remote-1", null));
		provider.upsert(3L, request(null));
		verify(googleApiClient).createEvent(eq("token"), eq("cal-1"), any());
	}

	private CalendarEventRequest request(String externalId) {
		return new CalendarEventRequest("event-1", externalId, "Conference", null, "Bangalore",
				Instant.parse("2026-10-20T02:00:00Z"), Instant.parse("2026-10-20T04:00:00Z"), "Asia/Singapore", "confirmed");
	}
}
