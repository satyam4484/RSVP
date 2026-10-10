package com.rsvp.calendar.entity;

import com.rsvp.common.audit.PublicEntity;
import com.rsvp.common.enums.CalendarProviderType;
import com.rsvp.common.enums.SyncStatus;
import com.rsvp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "calendar_connections")
public class CalendarConnection extends PublicEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "external_account_id")
	private ExternalAccountConnection externalAccount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private CalendarProviderType provider;

	@Column(length = 255)
	private String externalCalendarId;

	@Column(length = 255)
	private String calendarName;

	@Column(nullable = false)
	private boolean isDefault = false;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(length = 64)
	private String feedToken;

	@Column(columnDefinition = "text")
	private String syncToken;

	private Instant lastSyncedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SyncStatus syncStatus;
}
