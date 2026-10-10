package com.rsvp.expense.repository;

import com.rsvp.expense.entity.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByUuidAndDeletedFalse(UUID uuid);

	List<Payment> findByExpense_IdAndDeletedFalseOrderByPaymentDateAsc(Long expenseId);

	@Query("""
			select payment from Payment payment
			where payment.deleted = false and payment.expense.id in :expenseIds
			""")
	List<Payment> findActiveByExpenseIds(List<Long> expenseIds);
}
