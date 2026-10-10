package com.rsvp.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@MappedSuperclass
public abstract class PublicEntity extends TimestampedEntity {

	@Column(nullable = false, updatable = false, length = 36)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID uuid;

	@PrePersist
	void assignUuid() {
		if (uuid == null) {
			uuid = UUID.randomUUID();
		}
	}
}
