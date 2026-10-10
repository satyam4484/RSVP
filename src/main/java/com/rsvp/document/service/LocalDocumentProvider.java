package com.rsvp.document.service;

import com.rsvp.common.config.RsvpProperties;
import com.rsvp.common.enums.StorageProvider;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalDocumentProvider implements DocumentProvider {

	private final Path root;

	public LocalDocumentProvider(RsvpProperties properties) {
		this.root = Path.of(properties.getStorage().getLocalDir()).toAbsolutePath().normalize();
	}

	@Override
	public StorageProvider provider() {
		return StorageProvider.LOCAL;
	}

	@Override
	public ExternalDocument attach(Long userId, String externalFileId, String fallbackName, byte[] content, String mimeType) {
		if (content == null || content.length == 0) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File is empty");
		}
		String relative = userId + "/" + UUID.randomUUID();
		Path target = resolve(relative);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		} catch (IOException ex) {
			throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED, "File could not be stored");
		}
		return new ExternalDocument(fallbackName, mimeType == null ? "application/octet-stream" : mimeType,
				(long) content.length, relative, null);
	}

	@Override
	public byte[] read(Long userId, String externalFileId) {
		Path target = resolve(externalFileId);
		if (!externalFileId.startsWith(userId + "/")) {
			throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "Document not found");
		}
		try {
			return Files.readAllBytes(target);
		} catch (IOException ex) {
			throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "Document not found");
		}
	}

	private Path resolve(String relative) {
		Path target = root.resolve(relative).normalize();
		if (!target.startsWith(root)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid document path");
		}
		return target;
	}
}
