package com.rsvp.user.entity;

import com.rsvp.common.audit.PublicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends PublicEntity {

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(length = 32)
	private String phone;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, length = 64)
	private String timezone;

	@Column(nullable = false, length = 35)
	private String locale;

	@Column(length = 1000)
	private String profileImageUrl;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private boolean deleted = false;

	private Instant deletedAt;

	@Column(nullable = false)
	private long deletedMarker = 0L;

	public void markDeleted() {
		this.deleted = true;
		this.deletedAt = Instant.now();
		if (getId() != null) {
			this.deletedMarker = getId();
		}
	}
}
