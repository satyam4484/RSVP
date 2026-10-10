package com.rsvp.user.service;

import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.common.util.Timezones;
import com.rsvp.user.dto.UpdateUserRequest;
import com.rsvp.user.dto.UserResponse;
import com.rsvp.user.entity.User;
import com.rsvp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public User requireActive(Long userId) {
		User user = userRepository.findByIdAndDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
		if (!user.isActive()) {
			throw new ResourceNotFoundException(ErrorCode.ACCOUNT_INACTIVE, "Account is inactive");
		}
		return user;
	}

	@Transactional(readOnly = true)
	public UserResponse get(Long userId) {
		return toResponse(requireActive(userId));
	}

	@Transactional
	public UserResponse update(Long userId, UpdateUserRequest request) {
		User user = requireActive(userId);
		user.setName(request.name().trim());
		user.setPhone(blankToNull(request.phone()));
		user.setTimezone(Timezones.require(request.timezone()).getId());
		user.setLocale(request.locale() == null || request.locale().isBlank() ? user.getLocale() : request.locale().trim());
		user.setProfileImageUrl(blankToNull(request.profileImageUrl()));
		return toResponse(user);
	}

	public UserResponse toResponse(User user) {
		return new UserResponse(user.getUuid(), user.getName(), user.getEmail(), user.getPhone(), user.getTimezone(),
				user.getLocale(), user.getProfileImageUrl(), user.isActive());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
