package com.rsvp.calendar.entity;

import com.rsvp.common.audit.TimestampedEntity;
import com.rsvp.common.enums.SyncStatus;
import com.rsvp.common.enums.SyncSubject;
import com.rsvp.event.entity.Event;
import com.rsvp.task.entity.Task;
import com.rsvp.travel.entity.Travel;
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
@Table(name = "calendar_event_mappings")
public class CalendarEventMapping extends TimestampedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "calendar_connection_id", nullable = false)
	private CalendarConnection calendarConnection;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SyncSubject subjectType;

	@Column(nullable = false)
	private Long subjectId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id")
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "travel_id")
	private Travel travel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id")
	private Task task;

	@Column(nullable = false, length = 120)
	private String idempotencyKey;

	@Column(length = 255)
	private String externalEventId;

	@Column(length = 1000)
	private String externalEventUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SyncStatus syncStatus;

	@Column(length = 500)
	private String syncError;

	@Column(nullable = false)
	private int attemptCount = 0;

	private Instant lastSyncedAt;
}
