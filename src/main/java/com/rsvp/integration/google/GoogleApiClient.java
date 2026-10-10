package com.rsvp.integration.google;

import com.rsvp.calendar.service.CalendarProvider.CalendarEventRequest;
import com.rsvp.calendar.service.CalendarProvider.ExternalCalendar;
import com.rsvp.calendar.service.CalendarProvider.ExternalCalendarEvent;
import com.rsvp.common.exception.IntegrationException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GoogleApiClient {

	private static final DateTimeFormatter OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private final RestClient restClient;

	public GoogleApiClient(RestClient.Builder builder) {
		this.restClient = builder.build();
	}

	public GoogleToken exchangeCode(String clientId, String clientSecret, String redirectUri, String code) {
		return postToken(Map.of(
				"code", code,
				"client_id", clientId,
				"client_secret", clientSecret,
				"redirect_uri", redirectUri,
				"grant_type", "authorization_code"));
	}

	public GoogleToken refresh(String clientId, String clientSecret, String refreshToken) {
		return postToken(Map.of(
				"refresh_token", refreshToken,
				"client_id", clientId,
				"client_secret", clientSecret,
				"grant_type", "refresh_token"));
	}

	public GoogleUser userInfo(String accessToken) {
		return get("https://www.googleapis.com/oauth2/v2/userinfo", accessToken, GoogleUser.class);
	}

	public List<ExternalCalendar> listCalendars(String accessToken) {
		GoogleList body = get("https://www.googleapis.com/calendar/v3/users/me/calendarList", accessToken, GoogleList.class);
		if (body == null || body.items() == null) {
			return List.of();
		}
		return body.items().stream()
				.map(item -> new ExternalCalendar(item.id(), item.summary(), Boolean.TRUE.equals(item.primary())))
				.toList();
	}

	public ExternalCalendar createCalendar(String accessToken, String summary, String timezone) {
		Map<String, Object> payload = Map.of("summary", summary, "timeZone", timezone);
		GoogleItem created = post("https://www.googleapis.com/calendar/v3/calendars", accessToken, payload, GoogleItem.class);
		return new ExternalCalendar(created.id(), created.summary(), false);
	}

	public List<ExternalCalendarEvent> findByKey(String accessToken, String calendarId, String key) {
		try {
			GoogleEventList body = restClient.get()
					.uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events?privateExtendedProperty={property}&maxResults=5",
							calendarId, "rsvpKey=" + key)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.onStatus(status -> status.value() == 404, (request, response) -> {
						throw new GoogleResourceNotFoundException();
					})
					.body(GoogleEventList.class);
			if (body == null || body.items() == null) {
				return List.of();
			}
			List<ExternalCalendarEvent> events = new ArrayList<>();
			for (GoogleEvent item : body.items()) {
				events.add(new ExternalCalendarEvent(item.id(), item.htmlLink()));
			}
			return events;
		} catch (RestClientResponseException ex) {
			throw failure(ex);
		}
	}

	public ExternalCalendarEvent createEvent(String accessToken, String calendarId, CalendarEventRequest request) {
		GoogleEvent created = post("https://www.googleapis.com/calendar/v3/calendars/" + encode(calendarId) + "/events",
				accessToken, eventBody(request), GoogleEvent.class);
		return new ExternalCalendarEvent(created.id(), created.htmlLink());
	}

	public ExternalCalendarEvent updateEvent(String accessToken, String calendarId, CalendarEventRequest request) {
		try {
			GoogleEvent updated = restClient.put()
					.uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}", calendarId,
							request.externalEventId())
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.body(eventBody(request))
					.retrieve()
					.onStatus(status -> status.value() == 404, (req, res) -> {
						throw new GoogleResourceNotFoundException();
					})
					.body(GoogleEvent.class);
			return new ExternalCalendarEvent(updated.id(), updated.htmlLink());
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 404) {
				throw new GoogleResourceNotFoundException();
			}
			throw failure(ex);
		}
	}

	public void deleteEvent(String accessToken, String calendarId, String eventId) {
		try {
			restClient.delete()
					.uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.onStatus(status -> status.value() == 404, (req, res) -> {
						throw new GoogleResourceNotFoundException();
					})
					.toBodilessEntity();
		} catch (GoogleResourceNotFoundException ex) {
			return;
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 404) {
				return;
			}
			throw failure(ex);
		}
	}

	public DriveFile driveFile(String accessToken, String fileId) {
		return get("https://www.googleapis.com/drive/v3/files/" + fileId + "?fields=id,name,mimeType,size,webViewLink",
				accessToken, DriveFile.class);
	}

	public byte[] driveContent(String accessToken, String fileId) {
		try {
			return restClient.get()
					.uri("https://www.googleapis.com/drive/v3/files/{fileId}?alt=media", fileId)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.body(byte[].class);
		} catch (RestClientResponseException ex) {
			throw failure(ex);
		}
	}

	private GoogleToken postToken(Map<String, String> form) {
		try {
			return restClient.post()
					.uri("https://oauth2.googleapis.com/token")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(formBody(form))
					.retrieve()
					.body(GoogleToken.class);
		} catch (RestClientResponseException ex) {
			throw new IntegrationException("Google token request failed");
		}
	}

	private <T> T get(String url, String accessToken, Class<T> type) {
		try {
			return restClient.get().uri(url).header("Authorization", "Bearer " + accessToken).retrieve().body(type);
		} catch (RestClientResponseException ex) {
			throw failure(ex);
		}
	}

	private <T> T post(String url, String accessToken, Object body, Class<T> type) {
		try {
			return restClient.post()
					.uri(url)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(type);
		} catch (RestClientResponseException ex) {
			throw failure(ex);
		}
	}

	private Map<String, Object> eventBody(CalendarEventRequest request) {
		ZoneId zone = ZoneId.of(request.timezone());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("summary", request.summary());
		body.put("description", request.description());
		body.put("location", request.location());
		body.put("status", request.status() == null ? "confirmed" : request.status());
		body.put("start", Map.of("dateTime", OFFSET.format(request.start().atZone(zone)), "timeZone", request.timezone()));
		body.put("end", Map.of("dateTime", OFFSET.format(request.end().atZone(zone)), "timeZone", request.timezone()));
		body.put("extendedProperties", Map.of("private", Map.of("rsvpKey", request.idempotencyKey())));
		return body;
	}

	private String formBody(Map<String, String> form) {
		return form.entrySet().stream()
				.map(entry -> entry.getKey() + "=" + java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8))
				.reduce((left, right) -> left + "&" + right)
				.orElse("");
	}

	private String encode(String value) {
		return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
	}

	private IntegrationException failure(RestClientResponseException ex) {
		return new IntegrationException("Google request failed with status " + ex.getStatusCode().value());
	}

	public record GoogleToken(String access_token, String refresh_token, Long expires_in, String scope) {
	}

	public record GoogleUser(String id, String email) {
	}

	public record GoogleList(List<GoogleItem> items) {
	}

	public record GoogleItem(String id, String summary, Boolean primary) {
	}

	public record GoogleEventList(List<GoogleEvent> items) {
	}

	public record GoogleEvent(String id, String htmlLink) {
	}

	public record DriveFile(String id, String name, String mimeType, Long size, String webViewLink) {
	}

	@SuppressWarnings("unused")
	private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
	};
}
