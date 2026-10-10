package com.rsvp.contact.entity;

import com.rsvp.common.audit.PublicEntity;
import com.rsvp.common.enums.AttendanceStatus;
import com.rsvp.common.enums.ParticipantType;
import com.rsvp.event.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "participants")
public class EventParticipant extends PublicEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contact_id", nullable = false)
	private Contact contact;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ParticipantType participantType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AttendanceStatus attendanceStatus;

	private Instant invitedAt;

	private Instant respondedAt;

	@Column(columnDefinition = "text")
	private String notes;
}
