package com.rsvp.event.entity;

import com.rsvp.common.audit.AuditableEntity;
import com.rsvp.common.enums.EventStatus;
import com.rsvp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "events")
public class Event extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(nullable = false)
	private Instant startDateTime;

	@Column(nullable = false)
	private Instant endDateTime;

	@Column(nullable = false, length = 64)
	private String timezone;

	@Column(length = 255)
	private String locationName;

	@Column(length = 500)
	private String address;

	@Column(precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(precision = 9, scale = 6)
	private BigDecimal longitude;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private EventStatus status;

	@Column(length = 32)
	private String color;

	@Column(columnDefinition = "text")
	private String notes;

	private Instant completedAt;

	private Instant archivedAt;

	@Column(nullable = false)
	private boolean deleted = false;

	private Instant deletedAt;

	public void markDeleted(Instant when) {
		this.deleted = true;
		this.deletedAt = when;
	}
}
