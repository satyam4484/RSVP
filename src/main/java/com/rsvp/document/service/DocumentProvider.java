package com.rsvp.document.service;

import com.rsvp.common.enums.StorageProvider;

public interface DocumentProvider {

	StorageProvider provider();

	ExternalDocument attach(Long userId, String externalFileId, String fallbackName, byte[] content, String mimeType);

	byte[] read(Long userId, String externalFileId);
}
