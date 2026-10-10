package com.rsvp.notification.repository;

import com.rsvp.notification.entity.Notification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Page<Notification> findByUser_IdAndReadAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Optional<Notification> findByUuidAndUser_Id(UUID uuid, Long userId);
}
