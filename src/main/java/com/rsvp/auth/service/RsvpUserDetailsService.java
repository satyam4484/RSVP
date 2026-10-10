package com.rsvp.auth.service;

import com.rsvp.user.entity.User;
import com.rsvp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RsvpUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) {
		User user = userRepository.findByEmailAndDeletedFalse(email.toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
		return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
				.password(user.getPasswordHash())
				.disabled(!user.isActive())
				.build();
	}
}
