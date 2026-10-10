package com.rsvp.expense.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.response.PagedResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.expense.dto.ExpenseRequest;
import com.rsvp.expense.dto.ExpenseResponse;
import com.rsvp.expense.dto.ExpenseSummaryResponse;
import com.rsvp.expense.dto.PaymentRequest;
import com.rsvp.expense.dto.PaymentResponse;
import com.rsvp.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Expenses")
public class ExpenseController {

	private final ExpenseService expenseService;

	@PostMapping("/api/v1/events/{eventId}/expenses")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an expense")
	@StandardApiErrors
	public ApiResponse<ExpenseResponse> create(@PathVariable UUID eventId, @Valid @RequestBody ExpenseRequest request) {
		return ApiResponse.of(expenseService.create(SecurityUtils.currentUserId(), eventId, request), "Expense created");
	}

	@GetMapping("/api/v1/events/{eventId}/expenses")
	@Operation(summary = "List expenses for an event")
	@StandardApiErrors
	public PagedResponse<ExpenseResponse> list(
			@PathVariable UUID eventId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "expenseDate,desc") String sort) {
		return PagedResponse.from(expenseService.list(SecurityUtils.currentUserId(), eventId, page, size, sort));
	}

	@GetMapping("/api/v1/events/{eventId}/expenses/summary")
	@Operation(summary = "Calculate expense totals from payment records")
	@StandardApiErrors
	public ApiResponse<ExpenseSummaryResponse> summary(@PathVariable UUID eventId) {
		return ApiResponse.of(expenseService.summarizeEvent(SecurityUtils.currentUserId(), eventId), "Expense summary");
	}

	@GetMapping("/api/v1/expenses/{expenseId}")
	@Operation(summary = "Get an expense")
	@StandardApiErrors
	public ApiResponse<ExpenseResponse> get(@PathVariable UUID expenseId) {
		return ApiResponse.of(expenseService.get(SecurityUtils.currentUserId(), expenseId), "Expense");
	}

	@PutMapping("/api/v1/expenses/{expenseId}")
	@Operation(summary = "Update an expense")
	@StandardApiErrors
	public ApiResponse<ExpenseResponse> update(@PathVariable UUID expenseId, @Valid @RequestBody ExpenseRequest request) {
		return ApiResponse.of(expenseService.update(SecurityUtils.currentUserId(), expenseId, request), "Expense updated");
	}

	@DeleteMapping("/api/v1/expenses/{expenseId}")
	@Operation(summary = "Soft-delete an expense and its payments")
	@StandardApiErrors
	public ApiResponse<Void> delete(@PathVariable UUID expenseId) {
		expenseService.delete(SecurityUtils.currentUserId(), expenseId);
		return ApiResponse.message("Expense deleted");
	}

	@PostMapping("/api/v1/expenses/{expenseId}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Record a full or partial payment")
	@StandardApiErrors
	public ApiResponse<PaymentResponse> pay(@PathVariable UUID expenseId, @Valid @RequestBody PaymentRequest request) {
		return ApiResponse.of(expenseService.addPayment(SecurityUtils.currentUserId(), expenseId, request), "Payment recorded");
	}

	@GetMapping("/api/v1/expenses/{expenseId}/payments")
	@Operation(summary = "List payments for an expense")
	@StandardApiErrors
	public ApiResponse<List<PaymentResponse>> payments(@PathVariable UUID expenseId) {
		return ApiResponse.of(expenseService.listPayments(SecurityUtils.currentUserId(), expenseId), "Payments");
	}

	@DeleteMapping("/api/v1/payments/{paymentId}")
	@Operation(summary = "Soft-delete a payment and recalculate the balance")
	@StandardApiErrors
	public ApiResponse<Void> deletePayment(@PathVariable UUID paymentId) {
		expenseService.deletePayment(SecurityUtils.currentUserId(), paymentId);
		return ApiResponse.message("Payment deleted");
	}
}
