package com.rsvp.common.security;

import com.rsvp.common.config.RsvpProperties;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

	private final int maxAttempts;
	private final int windowSeconds;
	private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

	public LoginRateLimitFilter(RsvpProperties properties) {
		this.maxAttempts = properties.getRateLimit().getLoginAttempts();
		this.windowSeconds = properties.getRateLimit().getWindowSeconds();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equalsIgnoreCase(request.getMethod())
				|| !request.getRequestURI().equals("/api/v1/auth/login");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws java.io.IOException, jakarta.servlet.ServletException {
		String key = clientKey(request);
		if (!allow(key)) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType("application/json");
			String body = """
					{"timestamp":"%s","status":429,"code":"RATE_LIMITED","message":"Too many login attempts","path":"%s"}
					""".formatted(Instant.now(), request.getRequestURI()).trim();
			response.getWriter().write(body);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean allow(String key) {
		long now = Instant.now().getEpochSecond();
		Deque<Long> window = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		synchronized (window) {
			while (!window.isEmpty() && now - window.peekFirst() >= windowSeconds) {
				window.removeFirst();
			}
			if (window.size() >= maxAttempts) {
				return false;
			}
			window.addLast(now);
			return true;
		}
	}

	private String clientKey(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
