package com.rsvp.contact.dto;

import com.rsvp.common.enums.VendorStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record EventContactRequest(
		@NotNull UUID contactId,
		@Size(max = 100) String role,
		@Size(max = 100) String vendorRole,
		VendorStatus status,
		@PositiveOrZero BigDecimal contractAmount,
		@Size(max = 3) String currency,
		@Size(max = 5000) String notes) {
}
