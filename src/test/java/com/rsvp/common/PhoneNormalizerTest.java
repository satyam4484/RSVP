package com.rsvp.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.util.PhoneNormalizer;
import org.junit.jupiter.api.Test;

class PhoneNormalizerTest {

	private final PhoneNormalizer normalizer = new PhoneNormalizer();

	@Test
	void normalizesEquivalentIndianNumbersToTheSameE164Value() {
		assertThat(normalizer.toE164("+91 98765 43210", "en-IN")).isEqualTo("+919876543210");
		assertThat(normalizer.toE164("+919876543210", "en-IN")).isEqualTo("+919876543210");
		assertThat(normalizer.toE164("09876543210", "en-IN")).isEqualTo("+919876543210");
	}

	@Test
	void rejectsANumberThatCannotBeParsed() {
		assertThatThrownBy(() -> normalizer.toE164("not-a-phone", "en-IN")).isInstanceOf(BusinessException.class);
	}
}
