package com.rsvp.task.entity;

import com.rsvp.common.audit.AuditableEntity;
import com.rsvp.common.enums.TaskPriority;
import com.rsvp.common.enums.TaskStatus;
import com.rsvp.contact.entity.Contact;
import com.rsvp.event.entity.Event;
import com.rsvp.user.entity.User;
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
@Table(name = "tasks")
public class Task extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TaskStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TaskPriority priority;

	private Instant dueAt;

	private Instant completedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_to_user_id")
	private User assignedTo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contact_id")
	private Contact contact;

	@Column(nullable = false)
	private boolean reminderEnabled = false;

	private Instant reminderAt;

	@Column(nullable = false)
	private boolean deleted = false;

	private Instant deletedAt;

	public void markDeleted(Instant when) {
		this.deleted = true;
		this.deletedAt = when;
	}
}
