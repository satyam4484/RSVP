package com.rsvp.common.exception;

public class CalendarSyncException extends ApiException {

	public CalendarSyncException(String message) {
		super(ErrorCode.CALENDAR_SYNC_FAILED, message);
	}

	public CalendarSyncException(String message, Throwable cause) {
		super(ErrorCode.CALENDAR_SYNC_FAILED, message);
		initCause(cause);
	}
}
