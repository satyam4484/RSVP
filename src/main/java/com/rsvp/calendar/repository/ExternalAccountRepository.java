package com.rsvp.calendar.repository;

import com.rsvp.calendar.entity.ExternalAccountConnection;
import com.rsvp.common.enums.ConnectionStatus;
import com.rsvp.common.enums.IntegrationProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalAccountRepository extends JpaRepository<ExternalAccountConnection, Long> {

	Optional<ExternalAccountConnection> findByUser_IdAndProvider(Long userId, IntegrationProvider provider);

	List<ExternalAccountConnection> findByStatusAndTokenExpiresAtBefore(ConnectionStatus status, Instant cutoff);
}
