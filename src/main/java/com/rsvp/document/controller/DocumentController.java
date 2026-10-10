package com.rsvp.document.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.document.dto.DocumentAccessResponse;
import com.rsvp.document.dto.DocumentResponse;
import com.rsvp.document.dto.DriveDocumentRequest;
import com.rsvp.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents")
public class DocumentController {

	private final DocumentService documentService;

	@PostMapping(value = "/api/v1/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Upload a document stored by RSVP")
	@StandardApiErrors
	public ApiResponse<DocumentResponse> upload(
			@RequestPart("file") MultipartFile file,
			@RequestParam(required = false) UUID eventId,
			@RequestParam(required = false) UUID travelId,
			@RequestParam(required = false) UUID expenseId,
			@RequestParam(required = false) UUID contactId,
			@RequestParam(required = false) UUID taskId) {
		return ApiResponse.of(documentService.upload(SecurityUtils.currentUserId(), file, eventId, travelId, expenseId,
				contactId, taskId), "Document uploaded");
	}

	@PostMapping("/api/v1/documents/google-drive")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Link an existing Google Drive file without copying it into RSVP")
	@StandardApiErrors
	public ApiResponse<DocumentResponse> linkDrive(@Valid @RequestBody DriveDocumentRequest request) {
		return ApiResponse.of(documentService.linkDrive(SecurityUtils.currentUserId(), request), "Drive document linked");
	}

	@GetMapping("/api/v1/documents/{documentId}")
	@Operation(summary = "Get document metadata")
	@StandardApiErrors
	public ApiResponse<DocumentResponse> get(@PathVariable UUID documentId) {
		return ApiResponse.of(documentService.get(SecurityUtils.currentUserId(), documentId), "Document");
	}

	@GetMapping("/api/v1/events/{eventId}/documents")
	@Operation(summary = "List documents for an event")
	@StandardApiErrors
	public ApiResponse<List<DocumentResponse>> list(@PathVariable UUID eventId) {
		return ApiResponse.of(documentService.listForEvent(SecurityUtils.currentUserId(), eventId), "Documents");
	}

	@GetMapping("/api/v1/documents/{documentId}/access")
	@Operation(summary = "Return a backend URL that opens the document without exposing tokens")
	@StandardApiErrors
	public ApiResponse<DocumentAccessResponse> access(@PathVariable UUID documentId) {
		return ApiResponse.of(documentService.access(SecurityUtils.currentUserId(), documentId), "Document access");
	}

	@GetMapping("/api/v1/documents/{documentId}/content")
	@Operation(summary = "Download document content through RSVP")
	@StandardApiErrors
	public ResponseEntity<byte[]> content(@PathVariable UUID documentId) {
		DocumentService.DocumentContent content = documentService.content(SecurityUtils.currentUserId(), documentId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(content.filename()).build().toString())
				.contentType(MediaType.parseMediaType(content.mimeType()))
				.body(content.bytes());
	}

	@DeleteMapping("/api/v1/documents/{documentId}")
	@Operation(summary = "Soft-delete a document")
	@StandardApiErrors
	public ApiResponse<Void> delete(@PathVariable UUID documentId) {
		documentService.delete(SecurityUtils.currentUserId(), documentId);
		return ApiResponse.message("Document deleted");
	}
}
