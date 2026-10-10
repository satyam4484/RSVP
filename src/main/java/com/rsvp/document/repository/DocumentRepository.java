package com.rsvp.document.repository;

import com.rsvp.document.entity.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	Optional<Document> findByUuidAndOwner_IdAndDeletedFalse(UUID uuid, Long ownerId);

	List<Document> findByEvent_IdAndDeletedFalse(Long eventId);

	long countByEvent_IdAndDeletedFalse(Long eventId);

	@Query("""
			select document from Document document
			where document.deleted = false and document.owner.id = :userId and lower(document.name) like :search
			""")
	List<Document> search(Long userId, String search, Pageable pageable);
}
