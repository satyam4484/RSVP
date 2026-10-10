package com.rsvp.contact.entity;

import com.rsvp.common.audit.AuditableEntity;
import com.rsvp.common.enums.ContactType;
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
@Table(name = "contacts")
public class Contact extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = 32)
	private String phone;

	@Column(nullable = false, length = 20)
	private String normalizedPhone;

	@Column(length = 255)
	private String email;

	@Column(length = 200)
	private String companyName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ContactType type;

	@Column(columnDefinition = "text")
	private String notes;

	@Column(nullable = false)
	private boolean deleted = false;

	private Instant deletedAt;

	@Column(nullable = false)
	private long deletedMarker = 0L;

	public void markDeleted(Instant when) {
		this.deleted = true;
		this.deletedAt = when;
		if (getId() != null) {
			this.deletedMarker = getId();
		}
	}
}
