package com.rsvp.expense.dto;

import com.rsvp.common.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
		UUID id,
		UUID expenseId,
		BigDecimal amount,
		String currency,
		LocalDate paymentDate,
		PaymentMethod paymentMethod,
		String reference,
		String notes) {
}
