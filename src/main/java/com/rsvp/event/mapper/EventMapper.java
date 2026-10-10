package com.rsvp.event.mapper;

import com.rsvp.event.dto.EventRequest;
import com.rsvp.event.dto.EventResponse;
import com.rsvp.event.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

	public void apply(EventRequest request, Event event, String timezone) {
		event.setName(request.name().trim());
		event.setDescription(request.description());
		event.setStartDateTime(request.startDateTime());
		event.setEndDateTime(request.endDateTime());
		event.setTimezone(timezone);
		event.setLocationName(request.locationName());
		event.setAddress(request.address());
		event.setLatitude(request.latitude());
		event.setLongitude(request.longitude());
		event.setColor(request.color());
		event.setNotes(request.notes());
	}

	public EventResponse toResponse(Event event) {
		return new EventResponse(event.getUuid(), event.getName(), event.getDescription(), event.getStartDateTime(),
				event.getEndDateTime(), event.getTimezone(), event.getLocationName(), event.getAddress(),
				event.getLatitude(), event.getLongitude(), event.getStatus(), event.getColor(), event.getNotes(),
				event.getCompletedAt(), event.getArchivedAt(), event.getCreatedAt(), event.getUpdatedAt());
	}
}
