package com.rsvp.common.util;

import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;

public final class Currencies {

	private Currencies() {
	}

	public static String require(String currency, String fallback) {
		String value = currency == null || currency.isBlank() ? fallback : currency.trim().toUpperCase();
		if (!value.matches("[A-Z]{3}")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Currency must be a 3-letter ISO code");
		}
		return value;
	}
}
