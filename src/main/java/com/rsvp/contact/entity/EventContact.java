package com.rsvp.contact.entity;

import com.rsvp.common.audit.TimestampedEntity;
import com.rsvp.common.enums.VendorStatus;
import com.rsvp.event.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_contacts")
public class EventContact extends TimestampedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contact_id", nullable = false)
	private Contact contact;

	@Column(length = 100)
	private String role;

	@Column(length = 100)
	private String vendorRole;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private VendorStatus status;

	@Column(precision = 19, scale = 4)
	private BigDecimal contractAmount;

	@Column(length = 3)
	private String currency;

	@Column(columnDefinition = "text")
	private String notes;
}
