package com.rsvp.contact.service;

import com.rsvp.common.enums.AttendanceStatus;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.contact.dto.ParticipantRequest;
import com.rsvp.contact.dto.ParticipantResponse;
import com.rsvp.contact.entity.Contact;
import com.rsvp.contact.entity.EventParticipant;
import com.rsvp.contact.repository.EventParticipantRepository;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipantService {

	private static final EnumSet<AttendanceStatus> RESPONDED = EnumSet.of(AttendanceStatus.CONFIRMED,
			AttendanceStatus.DECLINED, AttendanceStatus.MAYBE, AttendanceStatus.ATTENDED, AttendanceStatus.NO_SHOW);

	private final EventParticipantRepository participantRepository;
	private final EventService eventService;
	private final ContactService contactService;
	private final Clock clock;

	@Transactional
	public ParticipantResponse add(Long userId, UUID eventId, ParticipantRequest request) {
		Event event = eventService.require(userId, eventId);
		Contact contact = contactService.require(userId, request.contactId());
		if (participantRepository.findByEvent_IdAndContact_Id(event.getId(), contact.getId()).isPresent()) {
			throw new BusinessException(ErrorCode.DUPLICATE_ASSOCIATION, "Contact is already a participant of this event");
		}
		EventParticipant participant = new EventParticipant();
		participant.setEvent(event);
		participant.setContact(contact);
		apply(participant, request, true);
		return toResponse(participantRepository.save(participant));
	}

	@Transactional(readOnly = true)
	public List<ParticipantResponse> list(Long userId, UUID eventId) {
		Event event = eventService.require(userId, eventId);
		return participantRepository.findActiveByEventId(event.getId()).stream().map(this::toResponse).toList();
	}

	@Transactional
	public ParticipantResponse update(Long userId, UUID eventId, UUID participantId, ParticipantRequest request) {
		Event event = eventService.require(userId, eventId);
		EventParticipant participant = participantRepository.findByUuidAndEvent_Id(participantId, event.getId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PARTICIPANT_NOT_FOUND, "Participant not found"));
		if (!participant.getContact().getUuid().equals(request.contactId())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Participant contact cannot be changed");
		}
		apply(participant, request, false);
		return toResponse(participant);
	}

	@Transactional
	public void delete(Long userId, UUID eventId, UUID participantId) {
		Event event = eventService.require(userId, eventId);
		EventParticipant participant = participantRepository.findByUuidAndEvent_Id(participantId, event.getId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PARTICIPANT_NOT_FOUND, "Participant not found"));
		participantRepository.delete(participant);
	}

	private void apply(EventParticipant participant, ParticipantRequest request, boolean creating) {
		AttendanceStatus status = request.attendanceStatus() == null ? AttendanceStatus.INVITED : request.attendanceStatus();
		participant.setParticipantType(request.participantType());
		participant.setAttendanceStatus(status);
		participant.setNotes(request.notes());
		Instant now = clock.instant();
		if (creating || participant.getInvitedAt() == null) {
			participant.setInvitedAt(now);
		}
		if (RESPONDED.contains(status)) {
			participant.setRespondedAt(now);
		}
	}

	private ParticipantResponse toResponse(EventParticipant participant) {
		return new ParticipantResponse(participant.getUuid(), participant.getContact().getUuid(),
				participant.getContact().getName(), participant.getContact().getPhone(), participant.getParticipantType(),
				participant.getAttendanceStatus(), participant.getInvitedAt(), participant.getRespondedAt(),
				participant.getNotes());
	}
}
