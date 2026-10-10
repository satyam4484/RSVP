package com.rsvp.common.security;

import com.rsvp.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static Long currentUserId() {
		return currentJwt().getClaim("uid") instanceof Number number
				? number.longValue()
				: fail();
	}

	public static String currentUserUuid() {
		String subject = currentJwt().getSubject();
		if (subject == null || subject.isBlank()) {
			throw new UnauthorizedException("Authentication required");
		}
		return subject;
	}

	private static Jwt currentJwt() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new UnauthorizedException("Authentication required");
		}
		if (!"access".equals(jwt.getClaimAsString("type"))) {
			throw new UnauthorizedException("Authentication required");
		}
		return jwt;
	}

	private static Long fail() {
		throw new UnauthorizedException("Authentication required");
	}
}
