package com.rsvp.expense.dto;

import com.rsvp.common.enums.ExpenseCategory;
import com.rsvp.common.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
		UUID id,
		UUID eventId,
		String title,
		String description,
		BigDecimal amount,
		String currency,
		ExpenseCategory category,
		UUID paidByContactId,
		UUID vendorContactId,
		PaymentStatus paymentStatus,
		BigDecimal amountPaid,
		BigDecimal amountPending,
		LocalDate expenseDate,
		String notes,
		Instant createdAt,
		Instant updatedAt) {
}
