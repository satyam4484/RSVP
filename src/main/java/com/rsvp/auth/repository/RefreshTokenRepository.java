package com.rsvp.auth.repository;

import com.rsvp.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("update RefreshToken token set token.revoked = true where token.user.id = :userId and token.revoked = false")
	int revokeAllForUser(Long userId);

	@Query("""
			select token from RefreshToken token
			where token.user.id = :userId and token.revoked = false
			order by token.createdAt asc
			""")
	List<RefreshToken> findActiveByUserId(Long userId);

	@Modifying
	@Query("delete from RefreshToken token where token.expiresAt < :cutoff")
	int deleteExpiredBefore(Instant cutoff);
}
