package com.rsvp.travel.repository;

import com.rsvp.travel.entity.Travel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TravelRepository extends JpaRepository<Travel, Long> {

	Optional<Travel> findByUuidAndDeletedFalse(UUID uuid);

	Optional<Travel> findByUuidAndEvent_User_IdAndDeletedFalse(UUID uuid, Long userId);

	Page<Travel> findByEvent_IdAndDeletedFalse(Long eventId, Pageable pageable);

	List<Travel> findByEvent_IdAndDeletedFalse(Long eventId);

	List<Travel> findByEvent_User_IdAndDeletedFalse(Long userId);

	Optional<Travel> findByIdAndDeletedFalse(Long id);

	long countByEvent_IdAndDeletedFalse(Long eventId);

	@Query("""
			select travel from Travel travel
			where travel.deleted = false and travel.event.deleted = false and travel.event.user.id = :userId
			  and travel.departureAt is not null and travel.departureAt >= :from
			order by travel.departureAt asc
			""")
	List<Travel> findUpcoming(Long userId, Instant from, Pageable pageable);

	@Query("""
			select travel from Travel travel
			where travel.deleted = false and travel.event.deleted = false and travel.event.user.id = :userId
			  and (lower(travel.title) like :search or lower(coalesce(travel.fromLocation, '')) like :search
			       or lower(coalesce(travel.toLocation, '')) like :search
			       or lower(coalesce(travel.bookingReference, '')) like :search
			       or lower(coalesce(travel.providerName, '')) like :search)
			""")
	List<Travel> search(Long userId, String search, Pageable pageable);
}
