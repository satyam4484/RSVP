package com.rsvp.expense;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsvp.common.enums.PaymentStatus;
import com.rsvp.expense.entity.Expense;
import com.rsvp.expense.service.ExpenseCalculator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpenseCalculatorTest {

	@Test
	void derivesPartialFullAndPendingBalances() {
		assertThat(ExpenseCalculator.statusFor(new BigDecimal("80000"), BigDecimal.ZERO, false)).isEqualTo(PaymentStatus.PENDING);
		assertThat(ExpenseCalculator.statusFor(new BigDecimal("80000"), new BigDecimal("40000"), false))
				.isEqualTo(PaymentStatus.PARTIALLY_PAID);
		assertThat(ExpenseCalculator.statusFor(new BigDecimal("80000"), new BigDecimal("80000"), false)).isEqualTo(PaymentStatus.PAID);
	}

	@Test
	void sumsEventTotalsFromRecordsAndSkipsCancelledExpenses() {
		Expense venue = expense(1L, "183000.0000", PaymentStatus.PARTIALLY_PAID);
		Expense cancelled = expense(2L, "10.0000", PaymentStatus.CANCELLED);
		var totals = ExpenseCalculator.summarize(List.of(venue, cancelled), Map.of(1L, new BigDecimal("143000")));
		assertThat(totals).hasSize(1);
		assertThat(totals.get(0).totalExpense()).isEqualByComparingTo("183000.0000");
		assertThat(totals.get(0).totalPaid()).isEqualByComparingTo("143000");
		assertThat(totals.get(0).totalPending()).isEqualByComparingTo("40000.0000");
		assertThat(totals.get(0).currency()).isEqualTo("INR");
	}

	private Expense expense(Long id, String amount, PaymentStatus status) {
		Expense expense = new Expense();
		expense.setId(id);
		expense.setAmount(new BigDecimal(amount));
		expense.setCurrency("INR");
		expense.setPaymentStatus(status);
		expense.setDeleted(false);
		return expense;
	}
}
