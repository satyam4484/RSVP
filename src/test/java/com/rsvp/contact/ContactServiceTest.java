package com.rsvp.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsvp.common.enums.ContactType;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.util.PhoneNormalizer;
import com.rsvp.contact.dto.ContactRequest;
import com.rsvp.contact.dto.EventContactRequest;
import com.rsvp.contact.entity.Contact;
import com.rsvp.contact.repository.ContactRepository;
import com.rsvp.contact.repository.EventContactRepository;
import com.rsvp.contact.service.ContactService;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import com.rsvp.user.entity.User;
import com.rsvp.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

	@Mock
	private ContactRepository contactRepository;
	@Mock
	private EventContactRepository eventContactRepository;
	@Mock
	private UserService userService;
	@Mock
	private EventService eventService;

	private ContactService contactService;

	@BeforeEach
	void setUp() {
		contactService = new ContactService(contactRepository, eventContactRepository, userService, eventService,
				new PhoneNormalizer(), Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void createsAContactAndRejectsADuplicateNormalizedPhone() {
		User user = user();
		when(userService.requireActive(1L)).thenReturn(user);
		when(contactRepository.findByUser_IdAndNormalizedPhoneAndDeletedFalse(1L, "+919876543210")).thenReturn(Optional.empty());
		when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var created = contactService.create(1L, request("09876543210"));
		assertThat(created.normalizedPhone()).isEqualTo("+919876543210");

		Contact existing = new Contact();
		existing.setId(9L);
		when(contactRepository.findByUser_IdAndNormalizedPhoneAndDeletedFalse(1L, "+919876543210"))
				.thenReturn(Optional.of(existing));
		assertThatThrownBy(() -> contactService.create(1L, request("+91 98765 43210"))).isInstanceOf(BusinessException.class);
	}

	@Test
	void associatesTheSameContactWithTwoEvents() {
		User user = user();
		Contact contact = new Contact();
		contact.setId(4L);
		contact.setUuid(UUID.randomUUID());
		contact.setUser(user);
		contact.setName("ABC Catering");
		contact.setPhone("+919876543210");
		contact.setType(ContactType.VENDOR);
		Event wedding = event(10L);
		Event conference = event(11L);
		when(eventService.require(1L, wedding.getUuid())).thenReturn(wedding);
		when(eventService.require(1L, conference.getUuid())).thenReturn(conference);
		when(contactRepository.findByUuidAndUser_IdAndDeletedFalse(contact.getUuid(), 1L)).thenReturn(Optional.of(contact));
		when(eventContactRepository.findByEvent_IdAndContact_Id(any(), any())).thenReturn(Optional.empty());
		when(eventContactRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		contactService.link(1L, wedding.getUuid(), link(contact.getUuid()));
		contactService.link(1L, conference.getUuid(), link(contact.getUuid()));
		verify(eventContactRepository, org.mockito.Mockito.times(2)).save(any());
	}

	private ContactRequest request(String phone) {
		return new ContactRequest("ABC Catering", phone, null, "ABC", ContactType.VENDOR, null);
	}

	private EventContactRequest link(UUID contactId) {
		return new EventContactRequest(contactId, "CATERER", "CATERER", null, null, null, null);
	}

	private User user() {
		User user = new User();
		user.setId(1L);
		user.setLocale("en-IN");
		user.setActive(true);
		return user;
	}

	private Event event(Long id) {
		Event event = new Event();
		event.setId(id);
		event.setUuid(UUID.randomUUID());
		return event;
	}
}
