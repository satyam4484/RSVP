package com.rsvp.expense.service;

import com.rsvp.common.enums.PaymentStatus;
import com.rsvp.expense.entity.Expense;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExpenseCalculator {

	private ExpenseCalculator() {
	}

	public static BigDecimal money(BigDecimal amount) {
		return amount.setScale(4, RoundingMode.HALF_UP);
	}

	public static PaymentStatus statusFor(BigDecimal total, BigDecimal paid, boolean cancelled) {
		if (cancelled) {
			return PaymentStatus.CANCELLED;
		}
		if (paid.signum() <= 0) {
			return PaymentStatus.PENDING;
		}
		if (paid.compareTo(total) >= 0) {
			return PaymentStatus.PAID;
		}
		return PaymentStatus.PARTIALLY_PAID;
	}

	public static List<CurrencyTotals> summarize(List<Expense> expenses, Map<Long, BigDecimal> paidByExpense) {
		Map<String, CurrencyTotals> totals = new LinkedHashMap<>();
		for (Expense expense : expenses) {
			if (expense.isDeleted() || expense.getPaymentStatus() == PaymentStatus.CANCELLED) {
				continue;
			}
			CurrencyTotals bucket = totals.computeIfAbsent(expense.getCurrency(), CurrencyTotals::empty);
			BigDecimal paid = paidByExpense.getOrDefault(expense.getId(), BigDecimal.ZERO);
			bucket.add(expense.getAmount(), paid);
		}
		return new ArrayList<>(totals.values());
	}

	public static final class CurrencyTotals {
		private final String currency;
		private BigDecimal totalExpense = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		private BigDecimal totalPaid = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

		private CurrencyTotals(String currency) {
			this.currency = currency;
		}

		public static CurrencyTotals empty(String currency) {
			return new CurrencyTotals(currency);
		}

		public void add(BigDecimal expense, BigDecimal paid) {
			totalExpense = totalExpense.add(expense);
			totalPaid = totalPaid.add(paid);
		}

		public String currency() {
			return currency;
		}

		public BigDecimal totalExpense() {
			return totalExpense;
		}

		public BigDecimal totalPaid() {
			return totalPaid;
		}

		public BigDecimal totalPending() {
			BigDecimal pending = totalExpense.subtract(totalPaid);
			return pending.signum() < 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : pending;
		}
	}
}
