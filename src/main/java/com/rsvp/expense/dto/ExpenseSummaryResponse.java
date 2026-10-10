package com.rsvp.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseSummaryResponse(
		BigDecimal totalExpense,
		BigDecimal totalPaid,
		BigDecimal totalPending,
		String currency,
		List<CurrencySummary> byCurrency) {

	public record CurrencySummary(BigDecimal totalExpense, BigDecimal totalPaid, BigDecimal totalPending, String currency) {
	}
}
