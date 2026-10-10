package com.rsvp.common.event;

import com.rsvp.common.enums.SyncAction;
import com.rsvp.common.enums.SyncSubject;

public record CalendarSyncRequested(SyncSubject subject, Long subjectId, SyncAction action) {
}
