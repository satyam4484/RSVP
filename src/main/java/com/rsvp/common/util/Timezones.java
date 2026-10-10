package com.rsvp.common.util;

import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import java.time.DateTimeException;
import java.time.ZoneId;

public final class Timezones {

	private Timezones() {
	}

	public static ZoneId require(String timezone) {
		if (timezone == null || timezone.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_TIMEZONE, "Timezone is required");
		}
		try {
			return ZoneId.of(timezone.trim());
		} catch (DateTimeException ex) {
			throw new BusinessException(ErrorCode.INVALID_TIMEZONE, "Unknown timezone: " + timezone);
		}
	}
}
