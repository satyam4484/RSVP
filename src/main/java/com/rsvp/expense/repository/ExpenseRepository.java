package com.rsvp.expense.repository;

import com.rsvp.common.enums.PaymentStatus;
import com.rsvp.expense.entity.Expense;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	Optional<Expense> findByUuidAndDeletedFalse(UUID uuid);

	Optional<Expense> findByUuidAndEvent_User_IdAndDeletedFalse(UUID uuid, Long userId);

	Page<Expense> findByEvent_IdAndDeletedFalse(Long eventId, Pageable pageable);

	List<Expense> findByEvent_IdAndDeletedFalse(Long eventId);

	@Query("""
			select expense from Expense expense
			where expense.deleted = false and expense.event.deleted = false and expense.event.user.id = :userId
			""")
	List<Expense> findActiveByUser(Long userId);

	@Query("""
			select expense from Expense expense
			where expense.deleted = false and expense.event.deleted = false and expense.event.user.id = :userId
			  and expense.paymentStatus in :statuses
			order by expense.expenseDate asc
			""")
	List<Expense> findByUserAndStatuses(Long userId, Collection<PaymentStatus> statuses, Pageable pageable);
}
