package com.rsvp.common.exception;

public class BusinessException extends ApiException {

	public BusinessException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
