package com.rsvp.calendar.service;

import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.common.config.RsvpProperties;
import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.event.entity.Event;
import com.rsvp.event.repository.EventRepository;
import com.rsvp.event.service.EventService;
import com.rsvp.task.entity.Task;
import com.rsvp.task.repository.TaskRepository;
import com.rsvp.task.service.TaskService;
import com.rsvp.travel.entity.Travel;
import com.rsvp.travel.repository.TravelRepository;
import com.rsvp.travel.service.TravelService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IcsService {

	private final EventService eventService;
	private final TravelService travelService;
	private final TaskService taskService;
	private final EventRepository eventRepository;
	private final TravelRepository travelRepository;
	private final TaskRepository taskRepository;
	private final CalendarConnectionRepository calendarConnectionRepository;
	private final RsvpProperties properties;

	@Transactional(readOnly = true)
	public String eventCalendar(Long userId, UUID eventId) {
		Event event = eventService.require(userId, eventId);
		return IcsBuilder.calendar(List.of(eventComponent(event)));
	}

	@Transactional(readOnly = true)
	public String travelCalendar(Long userId, UUID travelId) {
		Travel travel = travelService.require(userId, travelId);
		if (travel.getDepartureAt() == null) {
			throw new ResourceNotFoundException(ErrorCode.TRAVEL_NOT_FOUND, "Travel has no departure time");
		}
		return IcsBuilder.calendar(List.of(travelComponent(travel)));
	}

	@Transactional(readOnly = true)
	public String taskCalendar(Long userId, UUID taskId) {
		Task task = taskService.require(userId, taskId);
		if (task.getDueAt() == null) {
			throw new ResourceNotFoundException(ErrorCode.TASK_NOT_FOUND, "Task has no due time");
		}
		return IcsBuilder.calendar(List.of(taskComponent(task)));
	}

	@Transactional(readOnly = true)
	public String feed(String token) {
		CalendarConnection connection = calendarConnectionRepository.findByFeedToken(token)
				.filter(CalendarConnection::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CALENDAR_NOT_FOUND, "Calendar feed not found"));
		Long userId = connection.getUser().getId();
		List<IcsBuilder.Component> components = new ArrayList<>();
		eventRepository.findByUser_IdAndDeletedFalse(userId).forEach(event -> components.add(eventComponent(event)));
		travelRepository.findByEvent_User_IdAndDeletedFalse(userId).stream()
				.filter(travel -> travel.getDepartureAt() != null)
				.forEach(travel -> components.add(travelComponent(travel)));
		taskRepository.findByEvent_User_IdAndDeletedFalse(userId).stream()
				.filter(task -> task.getDueAt() != null)
				.forEach(task -> components.add(taskComponent(task)));
		return IcsBuilder.calendar(components);
	}

	private IcsBuilder.Component eventComponent(Event event) {
		return new IcsBuilder.Component("event-" + event.getUuid() + "@rsvp", event.getStartDateTime(), event.getEndDateTime(),
				event.getName(), event.getDescription(), join(event.getLocationName(), event.getAddress()),
				properties.getPublicBaseUrl() + "/api/v1/events/" + event.getUuid(), status(event.getStatus()));
	}

	private IcsBuilder.Component travelComponent(Travel travel) {
		Instant end = travel.getArrivalAt() != null ? travel.getArrivalAt() : travel.getDepartureAt().plus(1, ChronoUnit.HOURS);
		String summary = travel.getFromLocation() != null && travel.getToLocation() != null
				? travel.getFromLocation() + " → " + travel.getToLocation()
				: travel.getTitle();
		return new IcsBuilder.Component("travel-" + travel.getUuid() + "@rsvp", travel.getDepartureAt(), end, summary,
				travel.getNotes(), travel.getFromLocation(), properties.getPublicBaseUrl() + "/api/v1/travels/" + travel.getUuid(),
				"CONFIRMED");
	}

	private IcsBuilder.Component taskComponent(Task task) {
		return new IcsBuilder.Component("task-" + task.getUuid() + "@rsvp", task.getDueAt(), task.getDueAt().plus(30, ChronoUnit.MINUTES),
				task.getTitle(), task.getDescription(), task.getEvent().getLocationName(),
				properties.getPublicBaseUrl() + "/api/v1/tasks/" + task.getUuid(),
				task.getStatus().name().equals("CANCELLED") ? "CANCELLED" : "CONFIRMED");
	}

	private String status(EventStatus status) {
		return switch (status) {
			case CANCELLED -> "CANCELLED";
			case PLANNING -> "TENTATIVE";
			default -> "CONFIRMED";
		};
	}

	private String join(String name, String address) {
		if (name == null || name.isBlank()) {
			return address;
		}
		if (address == null || address.isBlank()) {
			return name;
		}
		return name + ", " + address;
	}
}
