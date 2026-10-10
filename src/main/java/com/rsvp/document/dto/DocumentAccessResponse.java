package com.rsvp.document.dto;

import com.rsvp.common.enums.StorageProvider;

public record DocumentAccessResponse(String url, StorageProvider provider) {
}
