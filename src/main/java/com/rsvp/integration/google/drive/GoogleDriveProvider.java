package com.rsvp.integration.google.drive;

import com.rsvp.common.enums.StorageProvider;
import com.rsvp.common.exception.IntegrationException;
import com.rsvp.document.service.DocumentProvider;
import com.rsvp.document.service.ExternalDocument;
import com.rsvp.integration.google.GoogleAccountService;
import com.rsvp.integration.google.GoogleApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleDriveProvider implements DocumentProvider {

	private final GoogleApiClient googleApiClient;
	private final GoogleAccountService googleAccountService;

	@Override
	public StorageProvider provider() {
		return StorageProvider.GOOGLE_DRIVE;
	}

	@Override
	public ExternalDocument attach(Long userId, String externalFileId, String fallbackName, byte[] content, String mimeType) {
		if (externalFileId == null || externalFileId.isBlank()) {
			throw new IntegrationException("Google Drive file id is required");
		}
		String token = googleAccountService.accessTokenForUser(userId);
		GoogleApiClient.DriveFile file = googleApiClient.driveFile(token, externalFileId);
		String name = file.name() == null || file.name().isBlank() ? fallbackName : file.name();
		return new ExternalDocument(name, file.mimeType(), file.size(), file.id(), file.webViewLink());
	}

	@Override
	public byte[] read(Long userId, String externalFileId) {
		String token = googleAccountService.accessTokenForUser(userId);
		byte[] content = googleApiClient.driveContent(token, externalFileId);
		return content == null ? new byte[0] : content;
	}
}
