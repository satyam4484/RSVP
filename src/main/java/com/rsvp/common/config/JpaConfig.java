package com.rsvp.common.config;

import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@EnableAsync
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "utcDateTimeProvider")
public class JpaConfig {

	@Bean
	public AuditorAware<Long> auditorAware() {
		return () -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
				return Optional.empty();
			}
			Object uid = jwt.getClaim("uid");
			if (uid instanceof Number number) {
				return Optional.of(number.longValue());
			}
			return Optional.empty();
		};
	}

	@Bean
	public DateTimeProvider utcDateTimeProvider() {
		return () -> Optional.of(Instant.now());
	}
}
