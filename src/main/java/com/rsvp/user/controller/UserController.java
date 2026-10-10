package com.rsvp.user.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.user.dto.UpdateUserRequest;
import com.rsvp.user.dto.UserResponse;
import com.rsvp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

	private final UserService userService;

	@PutMapping("/me")
	@Operation(summary = "Update the current user profile")
	@StandardApiErrors
	public ApiResponse<UserResponse> updateMe(@Valid @RequestBody UpdateUserRequest request) {
		return ApiResponse.of(userService.update(SecurityUtils.currentUserId(), request), "Profile updated");
	}
}
