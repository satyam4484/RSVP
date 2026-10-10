package com.rsvp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 150) String name,
		@Email @NotBlank String email,
		@NotBlank @Size(min = 8, max = 72) String password,
		@Size(max = 32) String phone,
		@NotBlank @Size(max = 64) String timezone,
		@Size(max = 35) String locale) {
}
