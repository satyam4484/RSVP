package com.rsvp.expense.entity;

import com.rsvp.common.audit.AuditableEntity;
import com.rsvp.common.enums.ExpenseCategory;
import com.rsvp.common.enums.PaymentStatus;
import com.rsvp.contact.entity.Contact;
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
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "expenses")
public class Expense extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ExpenseCategory category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "paid_by_contact_id")
	private Contact paidBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vendor_contact_id")
	private Contact vendor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PaymentStatus paymentStatus;

	@Column(nullable = false)
	private LocalDate expenseDate;

	@Column(columnDefinition = "text")
	private String notes;

	@Column(nullable = false)
	private boolean deleted = false;

	private Instant deletedAt;

	public void markDeleted(Instant when) {
		this.deleted = true;
		this.deletedAt = when;
	}
}
