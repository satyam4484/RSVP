package com.rsvp.contact.dto;

import com.rsvp.common.enums.AttendanceStatus;
import com.rsvp.common.enums.ParticipantType;
import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
		UUID id,
		UUID contactId,
		String name,
		String phone,
		ParticipantType participantType,
		AttendanceStatus attendanceStatus,
		Instant invitedAt,
		Instant respondedAt,
		String notes) {
}
