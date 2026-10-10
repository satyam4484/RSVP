package com.rsvp.document.service;

import com.rsvp.common.config.RsvpProperties;
import com.rsvp.common.enums.StorageProvider;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.contact.service.ContactService;
import com.rsvp.document.dto.DocumentAccessResponse;
import com.rsvp.document.dto.DocumentResponse;
import com.rsvp.document.dto.DriveDocumentRequest;
import com.rsvp.document.entity.Document;
import com.rsvp.document.repository.DocumentRepository;
import com.rsvp.event.service.EventService;
import com.rsvp.expense.service.ExpenseService;
import com.rsvp.task.service.TaskService;
import com.rsvp.travel.service.TravelService;
import com.rsvp.user.entity.User;
import com.rsvp.user.service.UserService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final UserService userService;
	private final EventService eventService;
	private final TravelService travelService;
	private final ExpenseService expenseService;
	private final ContactService contactService;
	private final TaskService taskService;
	private final List<DocumentProvider> providers;
	private final RsvpProperties properties;
	private final Clock clock;

	@Transactional
	public DocumentResponse upload(Long userId, MultipartFile file, UUID eventId, UUID travelId, UUID expenseId,
			UUID contactId, UUID taskId) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File is required");
		}
		if (file.getSize() > properties.getStorage().getMaxBytes()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File exceeds the maximum size");
		}
		User owner = userService.requireActive(userId);
		Document document = base(owner, eventId, travelId, expenseId, contactId, taskId, userId);
		try {
			ExternalDocument stored = provider(StorageProvider.LOCAL).attach(userId, null, cleanName(file.getOriginalFilename()),
					file.getBytes(), file.getContentType());
			fill(document, stored, StorageProvider.LOCAL, cleanName(file.getOriginalFilename()));
		} catch (java.io.IOException ex) {
			throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED, "File could not be read");
		}
		return toResponse(documentRepository.save(document));
	}

	@Transactional
	public DocumentResponse linkDrive(Long userId, DriveDocumentRequest request) {
		User owner = userService.requireActive(userId);
		ExternalDocument stored = provider(StorageProvider.GOOGLE_DRIVE).attach(userId, request.externalFileId(),
				request.name(), null, null);
		Document document = base(owner, request.eventId(), request.travelId(), request.expenseId(), request.contactId(),
				request.taskId(), userId);
		String name = request.name() == null || request.name().isBlank() ? stored.name() : cleanName(request.name());
		fill(document, stored, StorageProvider.GOOGLE_DRIVE, name);
		return toResponse(documentRepository.save(document));
	}

	@Transactional(readOnly = true)
	public DocumentResponse get(Long userId, UUID documentId) {
		return toResponse(require(userId, documentId));
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> listForEvent(Long userId, UUID eventId) {
		var event = eventService.require(userId, eventId);
		return documentRepository.findByEvent_IdAndDeletedFalse(event.getId()).stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public DocumentAccessResponse access(Long userId, UUID documentId) {
		Document document = require(userId, documentId);
		String url = properties.getPublicBaseUrl() + "/api/v1/documents/" + document.getUuid() + "/content";
		return new DocumentAccessResponse(url, document.getStorageProvider());
	}

	@Transactional(readOnly = true)
	public DocumentContent content(Long userId, UUID documentId) {
		Document document = require(userId, documentId);
		byte[] bytes = provider(document.getStorageProvider()).read(userId, document.getExternalFileId());
		return new DocumentContent(bytes, document.getMimeType() == null ? "application/octet-stream" : document.getMimeType(),
				document.getName());
	}

	@Transactional
	public void delete(Long userId, UUID documentId) {
		require(userId, documentId).markDeleted(clock.instant());
	}

	public DocumentResponse toResponse(Document document) {
		return new DocumentResponse(document.getUuid(), document.getName(), document.getStorageProvider(),
				document.getMimeType(), document.getSize(),
				document.getEvent() == null ? null : document.getEvent().getUuid(),
				document.getTravel() == null ? null : document.getTravel().getUuid(),
				document.getExpense() == null ? null : document.getExpense().getUuid(),
				document.getContact() == null ? null : document.getContact().getUuid(),
				document.getTask() == null ? null : document.getTask().getUuid(), document.getCreatedAt());
	}

	private Document require(Long userId, UUID documentId) {
		return documentRepository.findByUuidAndOwner_IdAndDeletedFalse(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document not found"));
	}

	private Document base(User owner, UUID eventId, UUID travelId, UUID expenseId, UUID contactId, UUID taskId, Long userId) {
		Document document = new Document();
		document.setOwner(owner);
		if (eventId != null) {
			document.setEvent(eventService.require(userId, eventId));
		}
		if (travelId != null) {
			var travel = travelService.require(userId, travelId);
			document.setTravel(travel);
			document.setEvent(travel.getEvent());
		}
		if (expenseId != null) {
			var expense = expenseService.require(userId, expenseId);
			document.setExpense(expense);
			document.setEvent(expense.getEvent());
		}
		if (contactId != null) {
			document.setContact(contactService.require(userId, contactId));
		}
		if (taskId != null) {
			var task = taskService.require(userId, taskId);
			document.setTask(task);
			document.setEvent(task.getEvent());
		}
		return document;
	}

	private void fill(Document document, ExternalDocument stored, StorageProvider provider, String name) {
		document.setName(name);
		document.setMimeType(stored.mimeType());
		document.setSize(stored.size());
		document.setStorageProvider(provider);
		document.setExternalFileId(stored.externalFileId());
		document.setExternalUrl(stored.externalUrl());
	}

	private DocumentProvider provider(StorageProvider provider) {
		return providers.stream().filter(item -> item.provider() == provider).findFirst()
				.orElseThrow(() -> new BusinessException(ErrorCode.INTEGRATION_NOT_CONFIGURED,
						"Storage provider is not available"));
	}

	private String cleanName(String name) {
		if (name == null || name.isBlank()) {
			return "document";
		}
		String cleaned = name.replace("\\", "").replace("/", "").replace("\r", "").replace("\n", "");
		return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
	}

	public record DocumentContent(byte[] bytes, String mimeType, String filename) {
	}
}
