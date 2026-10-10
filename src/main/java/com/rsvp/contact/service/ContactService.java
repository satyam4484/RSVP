package com.rsvp.contact.service;

import com.rsvp.common.enums.ContactType;
import com.rsvp.common.enums.VendorStatus;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.common.util.Currencies;
import com.rsvp.common.util.PageRequests;
import com.rsvp.common.util.PhoneNormalizer;
import com.rsvp.common.util.SearchText;
import com.rsvp.contact.dto.ContactRequest;
import com.rsvp.contact.dto.ContactResponse;
import com.rsvp.contact.dto.EventContactRequest;
import com.rsvp.contact.dto.EventContactResponse;
import com.rsvp.contact.entity.Contact;
import com.rsvp.contact.entity.EventContact;
import com.rsvp.contact.repository.ContactRepository;
import com.rsvp.contact.repository.EventContactRepository;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import com.rsvp.user.entity.User;
import com.rsvp.user.service.UserService;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

	private static final Set<String> SORTS = Set.of("name", "createdAt", "companyName");

	private final ContactRepository contactRepository;
	private final EventContactRepository eventContactRepository;
	private final UserService userService;
	private final EventService eventService;
	private final PhoneNormalizer phoneNormalizer;
	private final Clock clock;

	@Transactional
	public ContactResponse create(Long userId, ContactRequest request) {
		User user = userService.requireActive(userId);
		String normalized = phoneNormalizer.toE164(request.phone(), user.getLocale());
		ensureUnique(userId, normalized, null);
		Contact contact = new Contact();
		contact.setUser(user);
		apply(contact, request, normalized);
		return toResponse(contactRepository.save(contact));
	}

	@Transactional(readOnly = true)
	public Page<ContactResponse> list(Long userId, ContactType type, String search, int page, int size, String sort) {
		Pageable pageable = PageRequests.of(page, size, sort, SORTS, "name,asc");
		String like = search == null || search.isBlank() ? null : SearchText.like(search);
		return contactRepository.search(userId, type, like, pageable).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public ContactResponse get(Long userId, UUID contactId) {
		return toResponse(require(userId, contactId));
	}

	@Transactional(readOnly = true)
	public ContactResponse searchByPhone(Long userId, String phone) {
		User user = userService.requireActive(userId);
		String normalized = phoneNormalizer.toE164(phone, user.getLocale());
		return contactRepository.findActiveByPhone(userId, normalized).map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CONTACT_NOT_FOUND, "Contact not found"));
	}

	@Transactional
	public ContactResponse update(Long userId, UUID contactId, ContactRequest request) {
		Contact contact = require(userId, contactId);
		String normalized = phoneNormalizer.toE164(request.phone(), contact.getUser().getLocale());
		ensureUnique(userId, normalized, contact.getId());
		apply(contact, request, normalized);
		return toResponse(contact);
	}

	@Transactional
	public void delete(Long userId, UUID contactId) {
		Contact contact = require(userId, contactId);
		contact.markDeleted(clock.instant());
	}

	@Transactional
	public EventContactResponse link(Long userId, UUID eventId, EventContactRequest request) {
		Event event = eventService.require(userId, eventId);
		Contact contact = require(userId, request.contactId());
		if (eventContactRepository.findByEvent_IdAndContact_Id(event.getId(), contact.getId()).isPresent()) {
			throw new BusinessException(ErrorCode.DUPLICATE_ASSOCIATION, "Contact is already linked to this event");
		}
		EventContact link = new EventContact();
		link.setEvent(event);
		link.setContact(contact);
		applyLink(link, contact, request);
		return toLinkResponse(eventContactRepository.save(link));
	}

	@Transactional
	public void unlink(Long userId, UUID eventId, UUID contactId) {
		Event event = eventService.require(userId, eventId);
		Contact contact = require(userId, contactId);
		EventContact link = eventContactRepository.findByEvent_IdAndContact_Id(event.getId(), contact.getId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CONTACT_NOT_FOUND, "Event contact not found"));
		eventContactRepository.delete(link);
	}

	@Transactional(readOnly = true)
	public List<EventContactResponse> listLinks(Long userId, UUID eventId) {
		Event event = eventService.require(userId, eventId);
		return eventContactRepository.findActiveByEventId(event.getId()).stream().map(this::toLinkResponse).toList();
	}

	@Transactional(readOnly = true)
	public Contact require(Long userId, UUID contactId) {
		return contactRepository.findByUuidAndUser_IdAndDeletedFalse(contactId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CONTACT_NOT_FOUND, "Contact not found"));
	}

	public ContactResponse toResponse(Contact contact) {
		return new ContactResponse(contact.getUuid(), contact.getName(), contact.getPhone(), contact.getNormalizedPhone(),
				contact.getEmail(), contact.getCompanyName(), contact.getType(), contact.getNotes(), contact.getCreatedAt(),
				contact.getUpdatedAt());
	}

	private void apply(Contact contact, ContactRequest request, String normalized) {
		contact.setName(request.name().trim());
		contact.setPhone(request.phone().trim());
		contact.setNormalizedPhone(normalized);
		contact.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());
		contact.setCompanyName(request.companyName());
		contact.setType(request.type());
		contact.setNotes(request.notes());
	}

	private void applyLink(EventContact link, Contact contact, EventContactRequest request) {
		link.setRole(request.role());
		link.setVendorRole(request.vendorRole() != null ? request.vendorRole() : request.role());
		link.setStatus(request.status() != null ? request.status()
				: contact.getType() == ContactType.VENDOR ? VendorStatus.PROSPECT : VendorStatus.CONFIRMED);
		link.setContractAmount(request.contractAmount());
		link.setCurrency(request.contractAmount() == null ? null : Currencies.require(request.currency(), "INR"));
		link.setNotes(request.notes());
	}

	private EventContactResponse toLinkResponse(EventContact link) {
		Contact contact = link.getContact();
		return new EventContactResponse(contact.getUuid(), contact.getName(), contact.getPhone(), contact.getCompanyName(),
				contact.getType(), link.getRole(), link.getVendorRole(), link.getStatus(), link.getContractAmount(),
				link.getCurrency(), link.getNotes());
	}

	private void ensureUnique(Long userId, String normalized, Long currentId) {
		contactRepository.findByUser_IdAndNormalizedPhoneAndDeletedFalse(userId, normalized).ifPresent(existing -> {
			if (currentId == null || !existing.getId().equals(currentId)) {
				throw new BusinessException(ErrorCode.DUPLICATE_CONTACT, "A contact with this phone number already exists");
			}
		});
	}
}
