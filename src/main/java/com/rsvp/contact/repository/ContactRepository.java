package com.rsvp.contact.repository;

import com.rsvp.common.enums.ContactType;
import com.rsvp.contact.entity.Contact;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContactRepository extends JpaRepository<Contact, Long> {

	Optional<Contact> findByUuidAndUser_IdAndDeletedFalse(UUID uuid, Long userId);

	Optional<Contact> findByUser_IdAndNormalizedPhoneAndDeletedFalse(Long userId, String normalizedPhone);

	@Query("""
			select contact from Contact contact
			where contact.user.id = :userId
			  and contact.deleted = false
			  and (:type is null or contact.type = :type)
			  and (:search is null or lower(contact.name) like :search
			       or lower(coalesce(contact.companyName, '')) like :search
			       or lower(coalesce(contact.email, '')) like :search
			       or contact.normalizedPhone like :search)
			""")
	Page<Contact> search(Long userId, ContactType type, String search, Pageable pageable);

	@Query("""
			select contact from Contact contact
			where contact.user.id = :userId and contact.deleted = false and contact.normalizedPhone = :phone
			""")
	Optional<Contact> findActiveByPhone(Long userId, String phone);
}
