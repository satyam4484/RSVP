package com.rsvp.contact.repository;

import com.rsvp.common.enums.ContactType;
import com.rsvp.contact.entity.EventContact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventContactRepository extends JpaRepository<EventContact, Long> {

	Optional<EventContact> findByEvent_IdAndContact_Id(Long eventId, Long contactId);

	@Query("""
			select link from EventContact link
			where link.event.id = :eventId and link.contact.deleted = false
			order by link.contact.name asc
			""")
	List<EventContact> findActiveByEventId(Long eventId);

	@Query("""
			select count(link) from EventContact link
			where link.event.id = :eventId and link.contact.deleted = false and link.contact.type = :type
			""")
	long countByEventAndType(Long eventId, ContactType type);
}
