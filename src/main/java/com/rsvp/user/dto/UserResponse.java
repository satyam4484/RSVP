package com.rsvp.user.dto;

import java.util.UUID;

public record UserResponse(
		UUID id,
		String name,
		String email,
		String phone,
		String timezone,
		String locale,
		String profileImageUrl,
		boolean active) {
}
