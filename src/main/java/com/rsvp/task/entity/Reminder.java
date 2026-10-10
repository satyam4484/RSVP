package com.rsvp.task.entity;

import com.rsvp.common.audit.PublicEntity;
import com.rsvp.common.enums.ReminderChannel;
import com.rsvp.common.enums.ReminderStatus;
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
@Table(name = "reminders")
public class Reminder extends PublicEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Column(nullable = false)
	private Instant reminderAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ReminderChannel channel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ReminderStatus status;

	private Instant sentAt;

	@Column(length = 500)
	private String errorMessage;
}
