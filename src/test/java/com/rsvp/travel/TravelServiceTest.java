package com.rsvp.travel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rsvp.common.enums.TravelType;
import com.rsvp.common.event.CalendarSyncRequested;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import com.rsvp.travel.dto.TravelRequest;
import com.rsvp.travel.entity.Travel;
import com.rsvp.travel.repository.TravelRepository;
import com.rsvp.travel.service.TravelService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

	@Mock
	private TravelRepository travelRepository;
	@Mock
	private EventService eventService;
	@Mock
	private ApplicationEventPublisher publisher;

	private TravelService travelService;

	@BeforeEach
	void setUp() {
		travelService = new TravelService(travelRepository, eventService, publisher,
				Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void storesFlightTrainBusAndCabDetailsAndPublishesCalendarSync() {
		Event event = new Event();
		event.setId(3L);
		event.setUuid(UUID.randomUUID());
		when(eventService.require(1L, event.getUuid())).thenReturn(event);
		when(travelRepository.save(any(Travel.class))).thenAnswer(invocation -> {
			Travel travel = invocation.getArgument(0);
			travel.setId(20L);
			travel.setUuid(UUID.randomUUID());
			return travel;
		});
		assertThat(create(event, TravelType.FLIGHT, Map.of("flightNumber", "AI123")).type()).isEqualTo(TravelType.FLIGHT);
		assertThat(create(event, TravelType.TRAIN, Map.of("trainNumber", "12951")).details()).containsEntry("trainNumber", "12951");
		assertThat(create(event, TravelType.BUS, Map.of("operator", "VRL")).type()).isEqualTo(TravelType.BUS);
		assertThat(create(event, TravelType.CAB, Map.of("provider", "Uber")).type()).isEqualTo(TravelType.CAB);
		org.mockito.Mockito.verify(publisher, org.mockito.Mockito.atLeastOnce()).publishEvent(any(CalendarSyncRequested.class));
	}

	@Test
	void rejectsArrivalBeforeDeparture() {
		Event event = new Event();
		event.setId(3L);
		event.setUuid(UUID.randomUUID());
		when(eventService.require(1L, event.getUuid())).thenReturn(event);
		TravelRequest request = new TravelRequest(TravelType.FLIGHT, "Return", "Delhi", "Mumbai",
				Instant.parse("2026-10-18T10:00:00Z"), Instant.parse("2026-10-18T08:00:00Z"), "Asia/Kolkata", null, null, null,
				Map.of());
		assertThatThrownBy(() -> travelService.create(1L, event.getUuid(), request)).isInstanceOf(BusinessException.class);
	}

	private com.rsvp.travel.dto.TravelResponse create(Event event, TravelType type, Map<String, Object> details) {
		TravelRequest request = new TravelRequest(type, type.name(), "Mumbai", "Delhi", Instant.parse("2026-10-18T03:00:00Z"),
				Instant.parse("2026-10-18T05:00:00Z"), "Asia/Kolkata", "ABC123", "Provider", null, details);
		return travelService.create(1L, event.getUuid(), request);
	}
}
