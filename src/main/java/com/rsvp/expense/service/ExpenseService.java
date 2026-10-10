package com.rsvp.expense.service;

import com.rsvp.common.enums.PaymentStatus;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.common.util.Currencies;
import com.rsvp.common.util.PageRequests;
import com.rsvp.contact.entity.Contact;
import com.rsvp.contact.service.ContactService;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import com.rsvp.expense.dto.ExpenseRequest;
import com.rsvp.expense.dto.ExpenseResponse;
import com.rsvp.expense.dto.ExpenseSummaryResponse;
import com.rsvp.expense.dto.PaymentRequest;
import com.rsvp.expense.dto.PaymentResponse;
import com.rsvp.expense.entity.Expense;
import com.rsvp.expense.entity.Payment;
import com.rsvp.expense.repository.ExpenseRepository;
import com.rsvp.expense.repository.PaymentRepository;
import com.rsvp.expense.service.ExpenseCalculator.CurrencyTotals;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

	private static final Set<String> SORTS = Set.of("expenseDate", "createdAt", "amount", "title");

	private final ExpenseRepository expenseRepository;
	private final PaymentRepository paymentRepository;
	private final EventService eventService;
	private final ContactService contactService;
	private final Clock clock;

	@Transactional
	public ExpenseResponse create(Long userId, UUID eventId, ExpenseRequest request) {
		Event event = eventService.require(userId, eventId);
		Expense expense = new Expense();
		expense.setEvent(event);
		expense.setPaymentStatus(PaymentStatus.PENDING);
		apply(userId, expense, request);
		return toResponse(expenseRepository.save(expense), BigDecimal.ZERO);
	}

	@Transactional(readOnly = true)
	public Page<ExpenseResponse> list(Long userId, UUID eventId, int page, int size, String sort) {
		Event event = eventService.require(userId, eventId);
		Pageable pageable = PageRequests.of(page, size, sort, SORTS, "expenseDate,desc");
		Page<Expense> expenses = expenseRepository.findByEvent_IdAndDeletedFalse(event.getId(), pageable);
		Map<Long, BigDecimal> paid = paidByExpense(expenses.getContent());
		return expenses.map(expense -> toResponse(expense, paid.getOrDefault(expense.getId(), BigDecimal.ZERO)));
	}

	@Transactional(readOnly = true)
	public ExpenseResponse get(Long userId, UUID expenseId) {
		Expense expense = require(userId, expenseId);
		return toResponse(expense, paid(expense.getId()));
	}

	@Transactional
	public ExpenseResponse update(Long userId, UUID expenseId, ExpenseRequest request) {
		Expense expense = require(userId, expenseId);
		apply(userId, expense, request);
		BigDecimal paid = paid(expense.getId());
		if (!Boolean.TRUE.equals(request.cancelled()) && paid.compareTo(expense.getAmount()) > 0) {
			throw new BusinessException(ErrorCode.PAYMENT_EXCEEDS_BALANCE, "Expense amount cannot be below payments already recorded");
		}
		refreshStatus(expense, paid, Boolean.TRUE.equals(request.cancelled()));
		return toResponse(expense, paid);
	}

	@Transactional
	public void delete(Long userId, UUID expenseId) {
		Expense expense = require(userId, expenseId);
		Instant now = clock.instant();
		expense.markDeleted(now);
		paymentRepository.findByExpense_IdAndDeletedFalseOrderByPaymentDateAsc(expense.getId())
				.forEach(payment -> payment.markDeleted(now));
	}

	@Transactional
	public PaymentResponse addPayment(Long userId, UUID expenseId, PaymentRequest request) {
		Expense expense = require(userId, expenseId);
		if (expense.getPaymentStatus() == PaymentStatus.CANCELLED) {
			throw new BusinessException(ErrorCode.EXPENSE_CANCELLED, "Cannot add a payment to a cancelled expense");
		}
		String currency = Currencies.require(request.currency(), expense.getCurrency());
		if (!currency.equals(expense.getCurrency())) {
			throw new BusinessException(ErrorCode.CURRENCY_MISMATCH, "Payment currency must match the expense");
		}
		BigDecimal amount = ExpenseCalculator.money(request.amount());
		BigDecimal alreadyPaid = paid(expense.getId());
		if (alreadyPaid.add(amount).compareTo(expense.getAmount()) > 0) {
			throw new BusinessException(ErrorCode.PAYMENT_EXCEEDS_BALANCE, "Payment exceeds the remaining balance");
		}
		Payment payment = new Payment();
		payment.setExpense(expense);
		payment.setAmount(amount);
		payment.setCurrency(currency);
		payment.setPaymentDate(request.paymentDate());
		payment.setPaymentMethod(request.paymentMethod());
		payment.setReference(request.reference());
		payment.setNotes(request.notes());
		Payment saved = paymentRepository.save(payment);
		refreshStatus(expense, alreadyPaid.add(amount), false);
		return toPayment(saved);
	}

	@Transactional(readOnly = true)
	public List<PaymentResponse> listPayments(Long userId, UUID expenseId) {
		Expense expense = require(userId, expenseId);
		return paymentRepository.findByExpense_IdAndDeletedFalseOrderByPaymentDateAsc(expense.getId()).stream()
				.map(this::toPayment).toList();
	}

	@Transactional
	public void deletePayment(Long userId, UUID paymentId) {
		Payment payment = paymentRepository.findByUuidAndDeletedFalse(paymentId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found"));
		Expense expense = payment.getExpense();
		if (!expense.getEvent().getUser().getId().equals(userId) || expense.isDeleted()) {
			throw new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found");
		}
		BigDecimal remainingPaid = paid(expense.getId()).subtract(payment.getAmount());
		payment.markDeleted(clock.instant());
		refreshStatus(expense, remainingPaid.max(BigDecimal.ZERO), expense.getPaymentStatus() == PaymentStatus.CANCELLED);
	}

	@Transactional(readOnly = true)
	public ExpenseSummaryResponse summarizeEvent(Long userId, UUID eventId) {
		Event event = eventService.require(userId, eventId);
		return summarize(expenseRepository.findByEvent_IdAndDeletedFalse(event.getId()));
	}

	@Transactional(readOnly = true)
	public ExpenseSummaryResponse summarizeUser(Long userId) {
		return summarize(expenseRepository.findActiveByUser(userId));
	}

	@Transactional(readOnly = true)
	public Expense require(Long userId, UUID expenseId) {
		return expenseRepository.findByUuidAndEvent_User_IdAndDeletedFalse(expenseId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EXPENSE_NOT_FOUND, "Expense not found"));
	}

	private void apply(Long userId, Expense expense, ExpenseRequest request) {
		expense.setTitle(request.title().trim());
		expense.setDescription(request.description());
		expense.setAmount(ExpenseCalculator.money(request.amount()));
		expense.setCurrency(Currencies.require(request.currency(), "INR"));
		expense.setCategory(request.category());
		expense.setExpenseDate(request.expenseDate());
		expense.setNotes(request.notes());
		expense.setPaidBy(resolveContact(userId, request.paidByContactId()));
		expense.setVendor(resolveContact(userId, request.vendorContactId()));
		if (Boolean.TRUE.equals(request.cancelled())) {
			expense.setPaymentStatus(PaymentStatus.CANCELLED);
		} else if (expense.getPaymentStatus() == PaymentStatus.CANCELLED) {
			refreshStatus(expense, paid(expense.getId()), false);
		}
	}

	private Contact resolveContact(Long userId, UUID contactId) {
		return contactId == null ? null : contactService.require(userId, contactId);
	}

	private void refreshStatus(Expense expense, BigDecimal paidAmount, boolean cancelled) {
		expense.setPaymentStatus(ExpenseCalculator.statusFor(expense.getAmount(), paidAmount, cancelled));
	}

	private BigDecimal paid(Long expenseId) {
		return paymentRepository.findByExpense_IdAndDeletedFalseOrderByPaymentDateAsc(expenseId).stream()
				.map(Payment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private Map<Long, BigDecimal> paidByExpense(List<Expense> expenses) {
		if (expenses.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = expenses.stream().map(Expense::getId).toList();
		return paymentRepository.findActiveByExpenseIds(ids).stream()
				.collect(Collectors.groupingBy(payment -> payment.getExpense().getId(),
						Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
	}

	private ExpenseSummaryResponse summarize(List<Expense> expenses) {
		Map<Long, BigDecimal> paid = paidByExpense(expenses);
		List<CurrencyTotals> totals = ExpenseCalculator.summarize(expenses, paid);
		List<ExpenseSummaryResponse.CurrencySummary> byCurrency = totals.stream()
				.map(item -> new ExpenseSummaryResponse.CurrencySummary(item.totalExpense(), item.totalPaid(),
						item.totalPending(), item.currency()))
				.toList();
		CurrencyTotals primary = totals.stream().max(Comparator.comparing(CurrencyTotals::totalExpense))
				.orElse(CurrencyTotals.empty("INR"));
		if (totals.isEmpty()) {
			primary = CurrencyTotals.empty("INR");
		}
		return new ExpenseSummaryResponse(primary.totalExpense(), primary.totalPaid(), primary.totalPending(),
				primary.currency(), byCurrency);
	}

	private ExpenseResponse toResponse(Expense expense, BigDecimal paidAmount) {
		BigDecimal pending = expense.getPaymentStatus() == PaymentStatus.CANCELLED
				? BigDecimal.ZERO
				: expense.getAmount().subtract(paidAmount).max(BigDecimal.ZERO);
		return new ExpenseResponse(expense.getUuid(), expense.getEvent().getUuid(), expense.getTitle(),
				expense.getDescription(), expense.getAmount(), expense.getCurrency(), expense.getCategory(),
				expense.getPaidBy() == null ? null : expense.getPaidBy().getUuid(),
				expense.getVendor() == null ? null : expense.getVendor().getUuid(), expense.getPaymentStatus(), paidAmount,
				pending, expense.getExpenseDate(), expense.getNotes(), expense.getCreatedAt(), expense.getUpdatedAt());
	}

	private PaymentResponse toPayment(Payment payment) {
		return new PaymentResponse(payment.getUuid(), payment.getExpense().getUuid(), payment.getAmount(),
				payment.getCurrency(), payment.getPaymentDate(), payment.getPaymentMethod(), payment.getReference(),
				payment.getNotes());
	}
}
