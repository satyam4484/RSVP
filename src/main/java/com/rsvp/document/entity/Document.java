package com.rsvp.document.entity;

import com.rsvp.common.audit.AuditableEntity;
import com.rsvp.common.enums.StorageProvider;
import com.rsvp.contact.entity.Contact;
import com.rsvp.event.entity.Event;
import com.rsvp.expense.entity.Expense;
import com.rsvp.task.entity.Task;
import com.rsvp.travel.entity.Travel;
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
@Table(name = "documents")
public class Document extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_user_id", nullable = false)
	private User owner;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id")
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "travel_id")
	private Travel travel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "expense_id")
	private Expense expense;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contact_id")
	private Contact contact;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id")
	private Task task;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(length = 150)
	private String mimeType;

	private Long size;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private StorageProvider storageProvider;

	@Column(length = 500)
	private String externalFileId;

	@Column(length = 1000)
	private String externalUrl;

	@Column(nullable = false)
	private boolean deleted = false;

	private Instant deletedAt;

	public void markDeleted(Instant when) {
		this.deleted = true;
		this.deletedAt = when;
	}
}
