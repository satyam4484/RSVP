package com.rsvp.common.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rsvp")
public class RsvpProperties {

	private String publicBaseUrl = "http://localhost:8080";
	private List<String> corsAllowedOrigins = new ArrayList<>();
	private final Jwt jwt = new Jwt();
	private final Encryption encryption = new Encryption();
	private final Storage storage = new Storage();
	private final RateLimit rateLimit = new RateLimit();
	private final Scheduling scheduling = new Scheduling();
	private final Google google = new Google();
	private final Apple apple = new Apple();

	@Getter
	@Setter
	public static class Jwt {
		private String secret;
		private long accessTokenMinutes = 15;
		private long refreshTokenDays = 14;
		private String issuer = "rsvp";
	}

	@Getter
	@Setter
	public static class Encryption {
		private String key;
	}

	@Getter
	@Setter
	public static class Storage {
		private String localDir = "./data/uploads";
		private long maxBytes = 20_971_520L;
	}

	@Getter
	@Setter
	public static class RateLimit {
		private int loginAttempts = 5;
		private int windowSeconds = 60;
	}

	@Getter
	@Setter
	public static class Scheduling {
		private boolean enabled = true;
		private long eventCompletionDelayMs = 600_000L;
		private long reminderDelayMs = 60_000L;
		private long calendarRetryDelayMs = 600_000L;
		private long tokenRefreshDelayMs = 900_000L;
		private long cleanupDelayMs = 86_400_000L;
	}

	@Getter
	@Setter
	public static class Google {
		private String clientId = "";
		private String clientSecret = "";
		private String redirectUri = "";
	}

	@Getter
	@Setter
	public static class Apple {
		private String clientId = "";
		private String clientSecret = "";
	}
}
