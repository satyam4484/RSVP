package com.rsvp.contact.repository;

import com.rsvp.common.enums.AttendanceStatus;
import com.rsvp.contact.entity.EventParticipant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

	Optional<EventParticipant> findByUuidAndEvent_Id(UUID uuid, Long eventId);

	Optional<EventParticipant> findByEvent_IdAndContact_Id(Long eventId, Long contactId);

	@Query("""
			select participant from EventParticipant participant
			where participant.event.id = :eventId and participant.contact.deleted = false
			order by participant.contact.name asc
			""")
	List<EventParticipant> findActiveByEventId(Long eventId);

	long countByEvent_Id(Long eventId);

	long countByEvent_IdAndAttendanceStatusIn(Long eventId, Collection<AttendanceStatus> statuses);
}
