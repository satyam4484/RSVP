package com.rsvp.expense.dto;

import com.rsvp.common.enums.ExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 5000) String description,
		@NotNull @Positive BigDecimal amount,
		@Size(min = 3, max = 3) String currency,
		@NotNull ExpenseCategory category,
		UUID paidByContactId,
		UUID vendorContactId,
		@NotNull LocalDate expenseDate,
		@Size(max = 5000) String notes,
		Boolean cancelled) {
}
