package com.rsvp.contact.dto;

import com.rsvp.common.enums.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 32) String phone,
		@Email @Size(max = 255) String email,
		@Size(max = 200) String companyName,
		@NotNull ContactType type,
		@Size(max = 5000) String notes) {
}
