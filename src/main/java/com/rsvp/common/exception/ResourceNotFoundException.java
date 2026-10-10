package com.rsvp.common.exception;

public class ResourceNotFoundException extends ApiException {

	public ResourceNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
