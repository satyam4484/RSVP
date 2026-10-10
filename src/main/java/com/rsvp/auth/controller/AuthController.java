package com.rsvp.auth.controller;

import com.rsvp.auth.dto.AuthResponse;
import com.rsvp.auth.dto.LoginRequest;
import com.rsvp.auth.dto.RefreshTokenRequest;
import com.rsvp.auth.dto.RegisterRequest;
import com.rsvp.auth.service.AuthService;
import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@SecurityRequirements
	@Operation(summary = "Register a user and return access and refresh tokens")
	@StandardApiErrors
	public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.of(authService.register(request), "Account created");
	}

	@PostMapping("/login")
	@SecurityRequirements
	@Operation(summary = "Login with email and password")
	@StandardApiErrors
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.of(authService.login(request), "Login successful");
	}

	@PostMapping("/refresh")
	@SecurityRequirements
	@Operation(summary = "Rotate a refresh token and issue a new access token")
	@StandardApiErrors
	public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.of(authService.refresh(request), "Token refreshed");
	}

	@PostMapping("/logout")
	@SecurityRequirements
	@Operation(summary = "Revoke a refresh token")
	@StandardApiErrors
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
		return ApiResponse.message("Logged out");
	}

	@GetMapping("/me")
	@Operation(summary = "Return the authenticated user")
	@StandardApiErrors
	public ApiResponse<UserResponse> me() {
		return ApiResponse.of(authService.me(SecurityUtils.currentUserId()), "Current user");
	}
}
