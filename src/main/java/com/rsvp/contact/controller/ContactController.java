package com.rsvp.contact.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.enums.ContactType;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.response.PagedResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.contact.dto.ContactRequest;
import com.rsvp.contact.dto.ContactResponse;
import com.rsvp.contact.dto.EventContactRequest;
import com.rsvp.contact.dto.EventContactResponse;
import com.rsvp.contact.dto.ParticipantRequest;
import com.rsvp.contact.dto.ParticipantResponse;
import com.rsvp.contact.service.ContactService;
import com.rsvp.contact.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Contacts")
public class ContactController {

	private final ContactService contactService;
	private final ParticipantService participantService;

	@PostMapping("/api/v1/contacts")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a reusable contact")
	@StandardApiErrors
	public ApiResponse<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
		return ApiResponse.of(contactService.create(SecurityUtils.currentUserId(), request), "Contact created");
	}

	@GetMapping("/api/v1/contacts")
	@Operation(summary = "List contacts")
	@StandardApiErrors
	public PagedResponse<ContactResponse> list(
			@RequestParam(required = false) ContactType type,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name,asc") String sort) {
		return PagedResponse.from(contactService.list(SecurityUtils.currentUserId(), type, search, page, size, sort));
	}

	@GetMapping("/api/v1/contacts/search")
	@Operation(summary = "Find a contact by phone number")
	@StandardApiErrors
	public ApiResponse<ContactResponse> search(@RequestParam String phone) {
		return ApiResponse.of(contactService.searchByPhone(SecurityUtils.currentUserId(), phone), "Contact");
	}

	@GetMapping("/api/v1/contacts/{contactId:[0-9a-fA-F\\-]{36}}")
	@Operation(summary = "Get a contact")
	@StandardApiErrors
	public ApiResponse<ContactResponse> get(@PathVariable UUID contactId) {
		return ApiResponse.of(contactService.get(SecurityUtils.currentUserId(), contactId), "Contact");
	}

	@PutMapping("/api/v1/contacts/{contactId:[0-9a-fA-F\\-]{36}}")
	@Operation(summary = "Update a contact")
	@StandardApiErrors
	public ApiResponse<ContactResponse> update(@PathVariable UUID contactId, @Valid @RequestBody ContactRequest request) {
		return ApiResponse.of(contactService.update(SecurityUtils.currentUserId(), contactId, request), "Contact updated");
	}

	@DeleteMapping("/api/v1/contacts/{contactId:[0-9a-fA-F\\-]{36}}")
	@Operation(summary = "Soft-delete a contact")
	@StandardApiErrors
	public ApiResponse<Void> delete(@PathVariable UUID contactId) {
		contactService.delete(SecurityUtils.currentUserId(), contactId);
		return ApiResponse.message("Contact deleted");
	}

	@PostMapping("/api/v1/events/{eventId}/contacts")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Associate an existing contact with an event")
	@StandardApiErrors
	public ApiResponse<EventContactResponse> link(@PathVariable UUID eventId,
			@Valid @RequestBody EventContactRequest request) {
		return ApiResponse.of(contactService.link(SecurityUtils.currentUserId(), eventId, request), "Contact linked");
	}

	@GetMapping("/api/v1/events/{eventId}/contacts")
	@Operation(summary = "List contacts linked to an event")
	@StandardApiErrors
	public ApiResponse<List<EventContactResponse>> links(@PathVariable UUID eventId) {
		return ApiResponse.of(contactService.listLinks(SecurityUtils.currentUserId(), eventId), "Event contacts");
	}

	@DeleteMapping("/api/v1/events/{eventId}/contacts/{contactId}")
	@Operation(summary = "Remove a contact from an event")
	@StandardApiErrors
	public ApiResponse<Void> unlink(@PathVariable UUID eventId, @PathVariable UUID contactId) {
		contactService.unlink(SecurityUtils.currentUserId(), eventId, contactId);
		return ApiResponse.message("Contact removed from event");
	}

	@PostMapping("/api/v1/events/{eventId}/participants")
	@ResponseStatus(HttpStatus.CREATED)
	@Tag(name = "Participants")
	@Operation(summary = "Add a participant to an event")
	@StandardApiErrors
	public ApiResponse<ParticipantResponse> addParticipant(@PathVariable UUID eventId,
			@Valid @RequestBody ParticipantRequest request) {
		return ApiResponse.of(participantService.add(SecurityUtils.currentUserId(), eventId, request), "Participant added");
	}

	@GetMapping("/api/v1/events/{eventId}/participants")
	@Tag(name = "Participants")
	@Operation(summary = "List event participants")
	@StandardApiErrors
	public ApiResponse<List<ParticipantResponse>> participants(@PathVariable UUID eventId) {
		return ApiResponse.of(participantService.list(SecurityUtils.currentUserId(), eventId), "Participants");
	}

	@PutMapping("/api/v1/events/{eventId}/participants/{participantId}")
	@Tag(name = "Participants")
	@Operation(summary = "Update participant attendance")
	@StandardApiErrors
	public ApiResponse<ParticipantResponse> updateParticipant(@PathVariable UUID eventId, @PathVariable UUID participantId,
			@Valid @RequestBody ParticipantRequest request) {
		return ApiResponse.of(participantService.update(SecurityUtils.currentUserId(), eventId, participantId, request),
				"Participant updated");
	}

	@DeleteMapping("/api/v1/events/{eventId}/participants/{participantId}")
	@Tag(name = "Participants")
	@Operation(summary = "Remove a participant")
	@StandardApiErrors
	public ApiResponse<Void> deleteParticipant(@PathVariable UUID eventId, @PathVariable UUID participantId) {
		participantService.delete(SecurityUtils.currentUserId(), eventId, participantId);
		return ApiResponse.message("Participant removed");
	}
}
