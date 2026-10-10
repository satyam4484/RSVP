package com.rsvp.event.repository;

import com.rsvp.common.enums.EventStatus;
import com.rsvp.event.entity.Event;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

	Optional<Event> findByUuidAndUser_IdAndDeletedFalse(UUID uuid, Long userId);

	Optional<Event> findByIdAndDeletedFalse(Long id);

	List<Event> findByStatusInAndEndDateTimeBeforeAndDeletedFalse(Collection<EventStatus> statuses, Instant now);

	List<Event> findTop10ByUser_IdAndDeletedFalseAndStatusInAndStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(
			Long userId, Collection<EventStatus> statuses, Instant from);

	List<Event> findTop10ByUser_IdAndDeletedFalseOrderByUpdatedAtDesc(Long userId);

	List<Event> findByUser_IdAndDeletedFalse(Long userId);
}
