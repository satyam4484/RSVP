package com.rsvp.travel.service;

import com.rsvp.common.enums.SyncAction;
import com.rsvp.common.enums.SyncSubject;
import com.rsvp.common.event.CalendarSyncRequested;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.common.util.PageRequests;
import com.rsvp.common.util.Timezones;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import com.rsvp.travel.dto.TravelRequest;
import com.rsvp.travel.dto.TravelResponse;
import com.rsvp.travel.entity.Travel;
import com.rsvp.travel.repository.TravelRepository;
import java.time.Clock;
import java.util.HashMap;
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
public class TravelService {

	private static final Set<String> SORTS = Set.of("departureAt", "createdAt", "title", "type");

	private final TravelRepository travelRepository;
	private final EventService eventService;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	@Transactional
	public TravelResponse create(Long userId, UUID eventId, TravelRequest request) {
		Event event = eventService.require(userId, eventId);
		Travel travel = new Travel();
		travel.setEvent(event);
		apply(travel, request);
		Travel saved = travelRepository.save(travel);
		eventPublisher.publishEvent(new CalendarSyncRequested(SyncSubject.TRAVEL, saved.getId(), SyncAction.UPSERT));
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public Page<TravelResponse> list(Long userId, UUID eventId, int page, int size, String sort) {
		Event event = eventService.require(userId, eventId);
		Pageable pageable = PageRequests.of(page, size, sort, SORTS, "departureAt,asc");
		return travelRepository.findByEvent_IdAndDeletedFalse(event.getId(), pageable).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public TravelResponse get(Long userId, UUID travelId) {
		return toResponse(require(userId, travelId));
	}

	@Transactional
	public TravelResponse update(Long userId, UUID travelId, TravelRequest request) {
		Travel travel = require(userId, travelId);
		apply(travel, request);
		eventPublisher.publishEvent(new CalendarSyncRequested(SyncSubject.TRAVEL, travel.getId(), SyncAction.UPSERT));
		return toResponse(travel);
	}

	@Transactional
	public void delete(Long userId, UUID travelId) {
		Travel travel = require(userId, travelId);
		travel.markDeleted(clock.instant());
		eventPublisher.publishEvent(new CalendarSyncRequested(SyncSubject.TRAVEL, travel.getId(), SyncAction.DELETE));
	}

	@Transactional(readOnly = true)
	public Travel require(Long userId, UUID travelId) {
		return travelRepository.findByUuidAndEvent_User_IdAndDeletedFalse(travelId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRAVEL_NOT_FOUND, "Travel not found"));
	}

	public TravelResponse toResponse(Travel travel) {
		return new TravelResponse(travel.getUuid(), travel.getEvent().getUuid(), travel.getType(), travel.getTitle(),
				travel.getFromLocation(), travel.getToLocation(), travel.getDepartureAt(), travel.getArrivalAt(),
				travel.getTimezone(), travel.getBookingReference(), travel.getProviderName(), travel.getNotes(),
				travel.getDetailsJson(), travel.getCreatedAt(), travel.getUpdatedAt());
	}

	private void apply(Travel travel, TravelRequest request) {
		if (request.departureAt() != null && request.arrivalAt() != null
				&& request.arrivalAt().isBefore(request.departureAt())) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "Arrival must not be before departure");
		}
		travel.setType(request.type());
		travel.setTitle(request.title().trim());
		travel.setFromLocation(request.fromLocation());
		travel.setToLocation(request.toLocation());
		travel.setDepartureAt(request.departureAt());
		travel.setArrivalAt(request.arrivalAt());
		travel.setTimezone(Timezones.require(request.timezone()).getId());
		travel.setBookingReference(request.bookingReference());
		travel.setProviderName(request.providerName());
		travel.setNotes(request.notes());
		travel.setDetailsJson(request.details() == null ? new HashMap<>() : new HashMap<>(request.details()));
	}
}
