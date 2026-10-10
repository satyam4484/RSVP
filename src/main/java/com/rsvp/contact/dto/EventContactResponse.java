package com.rsvp.contact.dto;

import com.rsvp.common.enums.ContactType;
import com.rsvp.common.enums.VendorStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record EventContactResponse(
		UUID contactId,
		String name,
		String phone,
		String companyName,
		ContactType type,
		String role,
		String vendorRole,
		VendorStatus status,
		BigDecimal contractAmount,
		String currency,
		String notes) {
}
