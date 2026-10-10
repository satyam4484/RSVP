package com.rsvp.event.dto;

import com.rsvp.common.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record EventStatusRequest(@NotNull EventStatus status) {
}
