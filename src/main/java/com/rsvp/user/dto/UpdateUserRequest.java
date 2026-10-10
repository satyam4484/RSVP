package com.rsvp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 32) String phone,
		@NotBlank @Size(max = 64) String timezone,
		@Size(max = 35) String locale,
		@Size(max = 1000) String profileImageUrl) {
}
