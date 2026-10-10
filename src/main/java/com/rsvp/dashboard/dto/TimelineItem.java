package com.rsvp.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TimelineItem(String type, Instant dateTime, String title, BigDecimal amount, UUID referenceId) {
}
