package com.rsvp.calendar.repository;

import com.rsvp.calendar.entity.CalendarEventMapping;
import com.rsvp.common.enums.SyncStatus;
import com.rsvp.common.enums.SyncSubject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventMappingRepository extends JpaRepository<CalendarEventMapping, Long> {

	Optional<CalendarEventMapping> findByCalendarConnection_IdAndSubjectTypeAndSubjectId(Long connectionId,
			SyncSubject subjectType, Long subjectId);

	List<CalendarEventMapping> findByEvent_Id(Long eventId);

	List<CalendarEventMapping> findBySyncStatusIn(Collection<SyncStatus> statuses);

	List<CalendarEventMapping> findByCalendarConnection_User_Id(Long userId);
}
