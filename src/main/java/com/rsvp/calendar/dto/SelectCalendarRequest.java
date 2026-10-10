package com.rsvp.calendar.dto;

import jakarta.validation.constraints.NotBlank;

public record SelectCalendarRequest(@NotBlank String externalCalendarId, Boolean enabled, Boolean isDefault) {
}
