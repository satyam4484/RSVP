package com.rsvp.calendar.entity;

import com.rsvp.common.enums.ConnectionStatus;
import com.rsvp.common.enums.IntegrationProvider;
import com.rsvp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "external_account_connections")
public class ExternalAccountConnection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private IntegrationProvider provider;

	@Column(length = 255)
	private String externalAccountId;

	@Column(length = 255)
	private String email;

	@Column(columnDefinition = "text")
	private String accessTokenEncrypted;

	@Column(columnDefinition = "text")
	private String refreshTokenEncrypted;

	private Instant tokenExpiresAt;

	@Column(length = 1000)
	private String scopes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ConnectionStatus status;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;
}
