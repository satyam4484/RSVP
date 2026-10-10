package com.rsvp.travel.entity;

import com.rsvp.common.audit.AuditableEntity;
import com.rsvp.common.enums.TravelType;
import com.rsvp.common.util.JsonMapConverter;
import com.rsvp.event.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "travel")
public class Travel extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TravelType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 255)
	private String fromLocation;

	@Column(length = 255)
	private String toLocation;

	private Instant departureAt;

	private Instant arrivalAt;

	@Column(nullable = false, length = 64)
	private String timezone;

	@Column(length = 100)
	private String bookingReference;

	@Column(length = 200)
	private String providerName;

	@Column(columnDefinition = "text")
	private String notes;

	@Convert(converter = JsonMapConverter.class)
	@Column(columnDefinition = "longtext")
	private Map<String, Object> detailsJson = new HashMap<>();

	@Column(nullable = false)
	private boolean deleted = false;

	private Instant deletedAt;

	public void markDeleted(Instant when) {
		this.deleted = true;
		this.deletedAt = when;
	}
}
