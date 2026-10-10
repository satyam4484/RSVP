package com.rsvp.common.exception;

public class UnauthorizedException extends ApiException {

	public UnauthorizedException(String message) {
		super(ErrorCode.UNAUTHORIZED, message);
	}
}
