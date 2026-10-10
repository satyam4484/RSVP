package com.rsvp.user.repository;

import com.rsvp.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailAndDeletedFalse(String email);

	Optional<User> findByIdAndDeletedFalse(Long id);

	Optional<User> findByUuidAndDeletedFalse(UUID uuid);

	boolean existsByEmailAndDeletedFalse(String email);
}
