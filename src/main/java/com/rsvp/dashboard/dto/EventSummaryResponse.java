package com.rsvp.dashboard.dto;

import com.rsvp.common.enums.SyncStatus;
import com.rsvp.event.dto.EventResponse;
import java.math.BigDecimal;

public record EventSummaryResponse(
		EventResponse event,
		long participantCount,
		long confirmedParticipantCount,
		long vendorCount,
		long travelCount,
		long taskCount,
		long completedTaskCount,
		BigDecimal expenseTotal,
		BigDecimal paidAmount,
		BigDecimal pendingAmount,
		String currency,
		long documentCount,
		SyncStatus calendarSyncStatus) {
}
