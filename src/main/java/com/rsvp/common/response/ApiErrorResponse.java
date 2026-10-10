package com.rsvp.common.response;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		List<FieldErrorDetail> errors) {

	public ApiErrorResponse(Instant timestamp, int status, String code, String message, String path) {
		this(timestamp, status, code, message, path, null);
	}
}
