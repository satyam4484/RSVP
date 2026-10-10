package com.rsvp.common.exception;

public class IntegrationException extends ApiException {

	public IntegrationException(String message) {
		super(ErrorCode.INTEGRATION_FAILED, message);
	}

	public IntegrationException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
