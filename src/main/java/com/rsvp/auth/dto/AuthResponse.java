package com.rsvp.auth.dto;

import com.rsvp.user.dto.UserResponse;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		long expiresIn,
		String tokenType,
		UserResponse user) {
}
