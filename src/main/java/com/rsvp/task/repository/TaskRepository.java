package com.rsvp.task.repository;

import com.rsvp.common.enums.TaskPriority;
import com.rsvp.common.enums.TaskStatus;
import com.rsvp.task.entity.Task;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long> {

	Optional<Task> findByUuidAndDeletedFalse(UUID uuid);

	Optional<Task> findByUuidAndEvent_User_IdAndDeletedFalse(UUID uuid, Long userId);

	Optional<Task> findByIdAndDeletedFalse(Long id);

	List<Task> findByEvent_IdAndDeletedFalse(Long eventId);

	List<Task> findByEvent_User_IdAndDeletedFalse(Long userId);

	long countByEvent_IdAndDeletedFalse(Long eventId);

	long countByEvent_IdAndDeletedFalseAndStatus(Long eventId, TaskStatus status);

	@Query("""
			select task from Task task
			where task.deleted = false and task.event.id = :eventId
			  and (:status is null or task.status = :status)
			  and (:priority is null or task.priority = :priority)
			  and (:dueFrom is null or task.dueAt >= :dueFrom)
			  and (:dueTo is null or task.dueAt <= :dueTo)
			  and (:contactId is null or task.contact.id = :contactId)
			""")
	Page<Task> filter(Long eventId, TaskStatus status, TaskPriority priority, Instant dueFrom, Instant dueTo,
			Long contactId, Pageable pageable);

	@Query("""
			select task from Task task
			where task.deleted = false and task.event.deleted = false and task.event.user.id = :userId
			  and task.status in (com.rsvp.common.enums.TaskStatus.TODO, com.rsvp.common.enums.TaskStatus.IN_PROGRESS)
			  and task.dueAt >= :from and task.dueAt < :to
			order by task.dueAt asc
			""")
	List<Task> findDueBetween(Long userId, Instant from, Instant to);

	@Query("""
			select task from Task task
			where task.deleted = false and task.event.deleted = false and task.event.user.id = :userId
			  and task.status in (com.rsvp.common.enums.TaskStatus.TODO, com.rsvp.common.enums.TaskStatus.IN_PROGRESS)
			order by task.dueAt asc
			""")
	List<Task> findPending(Long userId, Pageable pageable);

	@Query("""
			select task from Task task
			where task.deleted = false and task.event.deleted = false and task.event.user.id = :userId
			  and (lower(task.title) like :search or lower(coalesce(task.description, '')) like :search)
			""")
	List<Task> search(Long userId, String search, Pageable pageable);
}
