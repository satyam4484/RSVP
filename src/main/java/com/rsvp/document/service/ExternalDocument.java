package com.rsvp.document.service;

import com.rsvp.common.enums.StorageProvider;

public record ExternalDocument(String name, String mimeType, Long size, String externalFileId, String externalUrl) {

	public StorageProvider provider() {
		return null;
	}
}
