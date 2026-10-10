package com.rsvp.task.repository;

import com.rsvp.common.enums.ReminderStatus;
import com.rsvp.task.entity.Reminder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

	Optional<Reminder> findByTask_IdAndStatus(Long taskId, ReminderStatus status);

	List<Reminder> findByTask_Id(Long taskId);

	List<Reminder> findTop100ByStatusAndReminderAtLessThanEqualOrderByReminderAtAsc(ReminderStatus status, Instant now);

	List<Reminder> findByEvent_Id(Long eventId);
}
