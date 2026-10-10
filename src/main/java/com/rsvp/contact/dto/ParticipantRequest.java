package com.rsvp.contact.dto;

import com.rsvp.common.enums.AttendanceStatus;
import com.rsvp.common.enums.ParticipantType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ParticipantRequest(
		@NotNull UUID contactId,
		@NotNull ParticipantType participantType,
		AttendanceStatus attendanceStatus,
		@Size(max = 5000) String notes) {
}
