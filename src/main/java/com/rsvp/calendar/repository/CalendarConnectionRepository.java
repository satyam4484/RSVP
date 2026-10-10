package com.rsvp.calendar.repository;

import com.rsvp.calendar.entity.CalendarConnection;
import com.rsvp.common.enums.CalendarProviderType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarConnectionRepository extends JpaRepository<CalendarConnection, Long> {

	List<CalendarConnection> findByUser_Id(Long userId);

	List<CalendarConnection> findByUser_IdAndEnabledTrue(Long userId);

	Optional<CalendarConnection> findByUuidAndUser_Id(UUID uuid, Long userId);

	Optional<CalendarConnection> findByFeedToken(String feedToken);

	Optional<CalendarConnection> findByUser_IdAndProvider(Long userId, CalendarProviderType provider);

	List<CalendarConnection> findByUser_IdAndProviderIn(Long userId, List<CalendarProviderType> providers);
}
