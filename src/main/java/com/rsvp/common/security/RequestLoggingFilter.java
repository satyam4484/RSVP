package com.rsvp.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
	private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9\\-]{1,64}");

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri.startsWith("/actuator");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws java.io.IOException, jakarta.servlet.ServletException {
		String requestId = request.getHeader("X-Request-Id");
		if (requestId == null || !SAFE_ID.matcher(requestId).matches()) {
			requestId = UUID.randomUUID().toString();
		}
		MDC.put("requestId", requestId);
		response.setHeader("X-Request-Id", requestId);
		long started = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		} finally {
			putUser();
			long durationMs = (System.nanoTime() - started) / 1_000_000;
			log.info("operation=http method={} path={} status={} durationMs={}",
					request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
			MDC.remove("userId");
			MDC.remove("requestId");
		}
	}

	private void putUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			Object uid = jwt.getClaim("uid");
			if (uid != null) {
				MDC.put("userId", String.valueOf(uid));
			}
		}
	}
}
