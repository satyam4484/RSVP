package com.rsvp.integration.google.calendar;

import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.calendar.service.CalendarProvider;
import com.rsvp.common.enums.CalendarProviderType;
import com.rsvp.common.exception.IntegrationException;
import com.rsvp.integration.google.GoogleAccountService;
import com.rsvp.integration.google.GoogleApiClient;
import com.rsvp.integration.google.GoogleResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleCalendarProvider implements CalendarProvider {

	private static final Logger log = LoggerFactory.getLogger(GoogleCalendarProvider.class);

	private final GoogleApiClient googleApiClient;
	private final GoogleAccountService googleAccountService;
	private final CalendarConnectionRepository calendarConnectionRepository;

	@Override
	public CalendarProviderType type() {
		return CalendarProviderType.GOOGLE;
	}

	@Override
	public ExternalCalendarEvent upsert(Long connectionId, CalendarEventRequest request) {
		Access access = access(connectionId);
		if (request.externalEventId() != null) {
			try {
				ExternalCalendarEvent updated = googleApiClient.updateEvent(access.token(), access.calendarId(), request);
				log.info("operation=calendar-upsert provider=GOOGLE success=true conflictPolicy=RSVP_WINS");
				return updated;
			} catch (GoogleResourceNotFoundException ex) {
				log.info("operation=calendar-upsert provider=GOOGLE outcome=missing-remote");
			}
		}
		List<ExternalCalendarEvent> found = googleApiClient.findByKey(access.token(), access.calendarId(), request.idempotencyKey());
		if (!found.isEmpty()) {
			CalendarEventRequest existing = new CalendarEventRequest(request.idempotencyKey(), found.get(0).id(),
					request.summary(), request.description(), request.location(), request.start(), request.end(),
					request.timezone(), request.status());
			ExternalCalendarEvent updated = googleApiClient.updateEvent(access.token(), access.calendarId(), existing);
			for (int index = 1; index < found.size(); index++) {
				googleApiClient.deleteEvent(access.token(), access.calendarId(), found.get(index).id());
			}
			log.info("operation=calendar-upsert provider=GOOGLE success=true duplicatePrevented=true");
			return updated;
		}
		ExternalCalendarEvent created = googleApiClient.createEvent(access.token(), access.calendarId(), request);
		log.info("operation=calendar-upsert provider=GOOGLE success=true");
		return created;
	}

	@Override
	public void delete(Long connectionId, String externalEventId, String idempotencyKey) {
		Access access = access(connectionId);
		if (externalEventId != null) {
			googleApiClient.deleteEvent(access.token(), access.calendarId(), externalEventId);
			return;
		}
		googleApiClient.findByKey(access.token(), access.calendarId(), idempotencyKey)
				.forEach(event -> googleApiClient.deleteEvent(access.token(), access.calendarId(), event.id()));
	}

	@Override
	public List<ExternalCalendar> listCalendars(Long userId) {
		return googleApiClient.listCalendars(googleAccountService.accessTokenForUser(userId));
	}

	private Access access(Long connectionId) {
		CalendarConnection connection = calendarConnectionRepository.findById(connectionId)
				.orElseThrow(() -> new IntegrationException("Calendar connection was not found"));
		if (connection.getExternalAccount() == null || connection.getExternalCalendarId() == null) {
			throw new IntegrationException("Google calendar is not selected");
		}
		return new Access(googleAccountService.accessToken(connection.getExternalAccount()), connection.getExternalCalendarId());
	}

	private record Access(String token, String calendarId) {
	}
}
