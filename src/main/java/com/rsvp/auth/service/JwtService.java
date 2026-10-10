package com.rsvp.auth.service;

import com.rsvp.common.config.RsvpProperties;
import com.rsvp.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final RsvpProperties properties;

	public JwtService(RsvpProperties properties) {
		SecretKey secretKey = new SecretKeySpec(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8),
				"HmacSHA256");
		this.jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
		this.properties = properties;
	}

	public String generateAccessToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.getJwt().getIssuer())
				.subject(user.getUuid().toString())
				.issuedAt(now)
				.expiresAt(now.plus(properties.getJwt().getAccessTokenMinutes(), ChronoUnit.MINUTES))
				.claim("uid", user.getId())
				.claim("email", user.getEmail())
				.claim("type", "access")
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long accessTokenSeconds() {
		return properties.getJwt().getAccessTokenMinutes() * 60;
	}
}
