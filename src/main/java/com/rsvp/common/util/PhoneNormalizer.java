package com.rsvp.common.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer {

	private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

	public String toE164(String rawPhone, String locale) {
		if (rawPhone == null || rawPhone.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PHONE, "Phone number is required");
		}
		String region = regionFromLocale(locale);
		try {
			var parsed = phoneNumberUtil.parse(rawPhone.trim(), region);
			if (!phoneNumberUtil.isValidNumber(parsed)) {
				throw new BusinessException(ErrorCode.INVALID_PHONE, "Phone number is not valid");
			}
			return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
		} catch (NumberParseException ex) {
			throw new BusinessException(ErrorCode.INVALID_PHONE, "Phone number could not be parsed");
		}
	}

	public String regionFromLocale(String locale) {
		if (locale == null || locale.isBlank()) {
			return "IN";
		}
		Locale parsed = Locale.forLanguageTag(locale.replace('_', '-'));
		if (parsed.getCountry() == null || parsed.getCountry().isBlank()) {
			return "IN";
		}
		return parsed.getCountry();
	}
}
