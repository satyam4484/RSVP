package com.rsvp.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rsvp.common.enums.ExpenseCategory;
import com.rsvp.common.enums.PaymentMethod;
import com.rsvp.common.enums.PaymentStatus;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import com.rsvp.expense.dto.ExpenseRequest;
import com.rsvp.expense.dto.PaymentRequest;
import com.rsvp.expense.entity.Expense;
import com.rsvp.expense.entity.Payment;
import com.rsvp.expense.repository.ExpenseRepository;
import com.rsvp.expense.repository.PaymentRepository;
import com.rsvp.expense.service.ExpenseService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;
	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private EventService eventService;
	@Mock
	private com.rsvp.contact.service.ContactService contactService;

	private ExpenseService expenseService;
	private Event event;
	private Expense expense;

	@BeforeEach
	void setUp() {
		expenseService = new ExpenseService(expenseRepository, paymentRepository, eventService, contactService,
				Clock.fixed(Instant.parse("2026-10-18T00:00:00Z"), ZoneOffset.UTC));
		event = new Event();
		event.setId(1L);
		event.setUuid(UUID.randomUUID());
		expense = new Expense();
		expense.setId(7L);
		expense.setUuid(UUID.randomUUID());
		expense.setEvent(event);
		expense.setAmount(new BigDecimal("80000.0000"));
		expense.setCurrency("INR");
		expense.setPaymentStatus(PaymentStatus.PENDING);
		expense.setTitle("Catering");
		expense.setCategory(ExpenseCategory.FOOD);
		expense.setExpenseDate(LocalDate.of(2026, 10, 18));
	}

	@Test
	void recordsAPartialPaymentThenAFullPaymentAndCalculatesThePendingBalance() {
		when(eventService.require(1L, event.getUuid())).thenReturn(event);
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
			Expense saved = invocation.getArgument(0);
			saved.setId(7L);
			saved.setUuid(expense.getUuid());
			return saved;
		});
		when(paymentRepository.findByExpense_IdAndDeletedFalseOrderByPaymentDateAsc(any())).thenReturn(List.of());
		var created = expenseService.create(1L, event.getUuid(), request(false));
		assertThat(created.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(created.amountPending()).isEqualByComparingTo("80000.0000");

		when(expenseRepository.findByUuidAndEvent_User_IdAndDeletedFalse(expense.getUuid(), 1L)).thenReturn(Optional.of(expense));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(3L);
			payment.setUuid(UUID.randomUUID());
			return payment;
		});
		var partial = expenseService.addPayment(1L, expense.getUuid(), payment("40000"));
		assertThat(partial.amount()).isEqualByComparingTo("40000");
		assertThat(expense.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

		when(paymentRepository.findByExpense_IdAndDeletedFalseOrderByPaymentDateAsc(7L)).thenReturn(List.of(storedPayment("40000")));
		expenseService.addPayment(1L, expense.getUuid(), payment("40000"));
		assertThat(expense.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

		when(expenseRepository.findByEvent_IdAndDeletedFalse(1L)).thenReturn(List.of(expense));
		when(paymentRepository.findActiveByExpenseIds(List.of(7L))).thenReturn(List.of(storedPayment("40000"), storedPayment("40000")));
		var summary = expenseService.summarizeEvent(1L, event.getUuid());
		assertThat(summary.totalExpense()).isEqualByComparingTo("80000.0000");
		assertThat(summary.totalPaid()).isEqualByComparingTo("80000");
		assertThat(summary.totalPending()).isEqualByComparingTo("0");
	}

	@Test
	void rejectsAPaymentAboveTheRemainingBalance() {
		when(expenseRepository.findByUuidAndEvent_User_IdAndDeletedFalse(expense.getUuid(), 1L)).thenReturn(Optional.of(expense));
		when(paymentRepository.findByExpense_IdAndDeletedFalseOrderByPaymentDateAsc(7L)).thenReturn(List.of(storedPayment("40000")));
		assertThatThrownBy(() -> expenseService.addPayment(1L, expense.getUuid(), payment("50000")))
				.isInstanceOf(BusinessException.class);
	}

	private ExpenseRequest request(boolean cancelled) {
		return new ExpenseRequest("Catering", null, new BigDecimal("80000"), "INR", ExpenseCategory.FOOD, null, null,
				LocalDate.of(2026, 10, 18), null, cancelled);
	}

	private PaymentRequest payment(String amount) {
		return new PaymentRequest(new BigDecimal(amount), "INR", LocalDate.of(2026, 10, 18), PaymentMethod.UPI, "UPI", null);
	}

	private Payment storedPayment(String amount) {
		Payment payment = new Payment();
		payment.setExpense(expense);
		payment.setAmount(new BigDecimal(amount));
		payment.setCurrency("INR");
		payment.setDeleted(false);
		return payment;
	}
}
