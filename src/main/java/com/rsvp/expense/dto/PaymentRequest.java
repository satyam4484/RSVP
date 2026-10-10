package com.rsvp.expense.dto;

import com.rsvp.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
		@NotNull @Positive BigDecimal amount,
		@Size(min = 3, max = 3) String currency,
		@NotNull LocalDate paymentDate,
		@NotNull PaymentMethod paymentMethod,
		@Size(max = 150) String reference,
		@Size(max = 5000) String notes) {
}
