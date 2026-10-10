package com.rsvp.integration.google;

import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.calendar.entity.ExternalAccountConnection;
import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.calendar.repository.ExternalAccountRepository;
import com.rsvp.common.config.RsvpProperties;
import com.rsvp.common.enums.CalendarProviderType;
import com.rsvp.common.enums.ConnectionStatus;
import com.rsvp.common.enums.IntegrationProvider;
import com.rsvp.common.enums.SyncStatus;
import com.rsvp.common.exception.IntegrationException;
import com.rsvp.common.util.EncryptionService;
import com.rsvp.common.util.TokenHasher;
import com.rsvp.integration.entity.OAuthState;
import com.rsvp.integration.google.GoogleApiClient.GoogleToken;
import com.rsvp.integration.google.GoogleApiClient.GoogleUser;
import com.rsvp.integration.repository.OAuthStateRepository;
import com.rsvp.user.entity.User;
import com.rsvp.user.repository.UserRepository;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleAccountService {

	static final String RSVP_CALENDAR = "RSVP Events";

	private final RsvpProperties properties;
	private final GoogleApiClient googleApiClient;
	private final ExternalAccountRepository externalAccountRepository;
	private final CalendarConnectionRepository calendarConnectionRepository;
	private final OAuthStateRepository oauthStateRepository;
	private final UserRepository userRepository;
	private final EncryptionService encryptionService;

	@Transactional
	public String authorizationUrl(Long userId) {
		requireConfigured();
		User user = userRepository.findByIdAndDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
		String state = TokenHasher.sha256(TokenHasher.randomToken()).substring(0, 48);
		OAuthState oauthState = new OAuthState();
		oauthState.setState(state);
		oauthState.setUser(user);
		oauthState.setProvider(IntegrationProvider.GOOGLE);
		oauthState.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
		oauthState.setCreatedAt(Instant.now());
		oauthStateRepository.save(oauthState);
		String scope = "openid email https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/drive.readonly";
		return "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&access_type=offline&prompt=consent"
				+ "&client_id=" + encode(properties.getGoogle().getClientId())
				+ "&redirect_uri=" + encode(properties.getGoogle().getRedirectUri())
				+ "&scope=" + encode(scope)
				+ "&state=" + encode(state);
	}

	@Transactional
	public void handleCallback(String code, String state) {
		requireConfigured();
		OAuthState oauthState = oauthStateRepository.findByState(state)
				.orElseThrow(() -> new IntegrationException("Google authorization state is invalid"));
		if (oauthState.getExpiresAt().isBefore(Instant.now())) {
			throw new IntegrationException("Google authorization state has expired");
		}
		GoogleToken token = googleApiClient.exchangeCode(properties.getGoogle().getClientId(),
				properties.getGoogle().getClientSecret(), properties.getGoogle().getRedirectUri(), code);
		GoogleUser profile = googleApiClient.userInfo(token.access_token());
		User user = oauthState.getUser();
		ExternalAccountConnection account = externalAccountRepository.findByUser_IdAndProvider(user.getId(), IntegrationProvider.GOOGLE)
				.orElseGet(ExternalAccountConnection::new);
		account.setUser(user);
		account.setProvider(IntegrationProvider.GOOGLE);
		account.setExternalAccountId(profile.id());
		account.setEmail(profile.email());
		account.setAccessTokenEncrypted(encryptionService.encrypt(token.access_token()));
		if (token.refresh_token() != null && !token.refresh_token().isBlank()) {
			account.setRefreshTokenEncrypted(encryptionService.encrypt(token.refresh_token()));
		}
		account.setTokenExpiresAt(Instant.now().plusSeconds(token.expires_in() == null ? 3600 : token.expires_in()));
		account.setScopes(token.scope());
		account.setStatus(ConnectionStatus.ACTIVE);
		account.setUpdatedAt(Instant.now());
		if (account.getCreatedAt() == null) {
			account.setCreatedAt(Instant.now());
		}
		ExternalAccountConnection saved = externalAccountRepository.save(account);
		ensureRsvpCalendar(saved, token.access_token(), user);
		oauthStateRepository.delete(oauthState);
	}

	@Transactional
	public void disconnect(Long userId) {
		externalAccountRepository.findByUser_IdAndProvider(userId, IntegrationProvider.GOOGLE).ifPresent(account -> {
			account.setStatus(ConnectionStatus.REVOKED);
			account.setAccessTokenEncrypted(null);
			account.setRefreshTokenEncrypted(null);
			account.setTokenExpiresAt(null);
			account.setUpdatedAt(Instant.now());
		});
		calendarConnectionRepository.findByUser_IdAndProvider(userId, CalendarProviderType.GOOGLE).ifPresent(connection -> {
			connection.setEnabled(false);
			connection.setSyncStatus(SyncStatus.NOT_CONNECTED);
		});
	}

	@Transactional
	public String accessToken(ExternalAccountConnection account) {
		if (account.getStatus() != ConnectionStatus.ACTIVE) {
			throw new IntegrationException("Google account is not connected");
		}
		if (account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
			return encryptionService.decrypt(account.getAccessTokenEncrypted());
		}
		String refresh = encryptionService.decrypt(account.getRefreshTokenEncrypted());
		if (refresh == null || refresh.isBlank()) {
			account.setStatus(ConnectionStatus.ERROR);
			throw new IntegrationException("Google refresh token is missing");
		}
		requireConfigured();
		GoogleToken token = googleApiClient.refresh(properties.getGoogle().getClientId(),
				properties.getGoogle().getClientSecret(), refresh);
		account.setAccessTokenEncrypted(encryptionService.encrypt(token.access_token()));
		account.setTokenExpiresAt(Instant.now().plusSeconds(token.expires_in() == null ? 3600 : token.expires_in()));
		account.setUpdatedAt(Instant.now());
		if (token.refresh_token() != null && !token.refresh_token().isBlank()) {
			account.setRefreshTokenEncrypted(encryptionService.encrypt(token.refresh_token()));
		}
		return token.access_token();
	}

	@Transactional
	public String accessTokenForUser(Long userId) {
		ExternalAccountConnection account = externalAccountRepository.findByUser_IdAndProvider(userId, IntegrationProvider.GOOGLE)
				.orElseThrow(() -> new IntegrationException(ErrorCode.INTEGRATION_NOT_CONFIGURED, "Google is not connected"));
		return accessToken(account);
	}

	@Transactional
	public int refreshExpiringTokens() {
		var expiring = externalAccountRepository.findByStatusAndTokenExpiresAtBefore(ConnectionStatus.ACTIVE,
				Instant.now().plus(10, ChronoUnit.MINUTES));
		int refreshed = 0;
		for (ExternalAccountConnection account : expiring) {
			if (account.getProvider() != IntegrationProvider.GOOGLE) {
				continue;
			}
			try {
				accessToken(account);
				refreshed++;
			} catch (RuntimeException ex) {
				account.setStatus(ConnectionStatus.ERROR);
				account.setUpdatedAt(Instant.now());
			}
		}
		return refreshed;
	}

	private void ensureRsvpCalendar(ExternalAccountConnection account, String accessToken, User user) {
		var calendars = googleApiClient.listCalendars(accessToken);
		var existing = calendars.stream().filter(calendar -> RSVP_CALENDAR.equals(calendar.name())).findFirst();
		var calendar = existing.orElseGet(() -> googleApiClient.createCalendar(accessToken, RSVP_CALENDAR, user.getTimezone()));
		CalendarConnection connection = calendarConnectionRepository.findByUser_IdAndProvider(user.getId(), CalendarProviderType.GOOGLE)
				.orElseGet(CalendarConnection::new);
		connection.setUser(user);
		connection.setExternalAccount(account);
		connection.setProvider(CalendarProviderType.GOOGLE);
		connection.setExternalCalendarId(calendar.id());
		connection.setCalendarName(calendar.name());
		connection.setDefault(true);
		connection.setEnabled(true);
		connection.setSyncStatus(SyncStatus.PENDING);
		calendarConnectionRepository.save(connection);
	}

	private void requireConfigured() {
		if (properties.getGoogle().getClientId() == null || properties.getGoogle().getClientId().isBlank()
				|| properties.getGoogle().getClientSecret() == null || properties.getGoogle().getClientSecret().isBlank()) {
			throw new IntegrationException(ErrorCode.INTEGRATION_NOT_CONFIGURED, "Google OAuth is not configured");
		}
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	public Optional<ExternalAccountConnection> find(Long userId) {
		return externalAccountRepository.findByUser_IdAndProvider(userId, IntegrationProvider.GOOGLE);
	}
}
