package com.rsvp.integration.repository;

import com.rsvp.integration.entity.OAuthState;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OAuthStateRepository extends JpaRepository<OAuthState, Long> {

	Optional<OAuthState> findByState(String state);

	@Modifying
	@Query("delete from OAuthState state where state.expiresAt < :cutoff")
	int deleteExpiredBefore(Instant cutoff);
}
