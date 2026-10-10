package com.rsvp.auth.service;

import com.rsvp.auth.dto.AuthResponse;
import com.rsvp.auth.dto.LoginRequest;
import com.rsvp.auth.dto.RefreshTokenRequest;
import com.rsvp.auth.dto.RegisterRequest;
import com.rsvp.auth.entity.RefreshToken;
import com.rsvp.auth.repository.RefreshTokenRepository;
import com.rsvp.common.config.RsvpProperties;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.UnauthorizedException;
import com.rsvp.common.util.Timezones;
import com.rsvp.common.util.TokenHasher;
import com.rsvp.user.dto.UserResponse;
import com.rsvp.user.entity.User;
import com.rsvp.user.repository.UserRepository;
import com.rsvp.user.service.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int MAX_REFRESH_TOKENS = 10;

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserService userService;
	private final RsvpProperties properties;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmailAndDeletedFalse(email)) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "An account with this email already exists");
		}
		User user = new User();
		user.setName(request.name().trim());
		user.setEmail(email);
		user.setPhone(blankToNull(request.phone()));
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setTimezone(Timezones.require(request.timezone()).getId());
		user.setLocale(request.locale() == null || request.locale().isBlank() ? "en" : request.locale().trim());
		user.setActive(true);
		user.setDeleted(false);
		User saved = userRepository.save(user);
		return issueTokens(saved);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase();
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
		User user = userRepository.findByEmailAndDeletedFalse(email)
				.orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
		return issueTokens(user);
	}

	@Transactional
	public AuthResponse refresh(RefreshTokenRequest request) {
		RefreshToken current = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(request.refreshToken()))
				.orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
		if (current.isRevoked() || current.getExpiresAt().isBefore(Instant.now())) {
			refreshTokenRepository.revokeAllForUser(current.getUser().getId());
			throw new UnauthorizedException("Invalid refresh token");
		}
		current.setRevoked(true);
		return issueTokens(current.getUser());
	}

	@Transactional
	public void logout(RefreshTokenRequest request) {
		refreshTokenRepository.findByTokenHash(TokenHasher.sha256(request.refreshToken())).ifPresent(token -> {
			if (Boolean.TRUE.equals(request.allSessions())) {
				refreshTokenRepository.revokeAllForUser(token.getUser().getId());
			} else {
				token.setRevoked(true);
			}
		});
	}

	@Transactional(readOnly = true)
	public UserResponse me(Long userId) {
		return userService.get(userId);
	}

	private AuthResponse issueTokens(User user) {
		if (!user.isActive() || user.isDeleted()) {
			throw new UnauthorizedException("Invalid email or password");
		}
		String refresh = TokenHasher.randomToken();
		RefreshToken token = new RefreshToken();
		token.setUser(user);
		token.setTokenHash(TokenHasher.sha256(refresh));
		token.setExpiresAt(Instant.now().plus(properties.getJwt().getRefreshTokenDays(), ChronoUnit.DAYS));
		token.setRevoked(false);
		token.setCreatedAt(Instant.now());
		refreshTokenRepository.save(token);
		trimSessions(user.getId());
		return new AuthResponse(jwtService.generateAccessToken(user), refresh, jwtService.accessTokenSeconds(), "Bearer",
				userService.toResponse(user));
	}

	private void trimSessions(Long userId) {
		List<RefreshToken> active = refreshTokenRepository.findActiveByUserId(userId);
		int overflow = active.size() - MAX_REFRESH_TOKENS;
		for (int i = 0; i < overflow; i++) {
			active.get(i).setRevoked(true);
		}
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
