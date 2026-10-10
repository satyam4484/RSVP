CREATE TABLE event_contacts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    contact_id BIGINT NOT NULL,
    role VARCHAR(100) NULL,
    vendor_role VARCHAR(100) NULL,
    status VARCHAR(32) NOT NULL,
    contract_amount DECIMAL(19, 4) NULL,
    currency CHAR(3) NULL,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_contacts_pair (event_id, contact_id),
    CONSTRAINT fk_event_contacts_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_event_contacts_contact FOREIGN KEY (contact_id) REFERENCES contacts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid CHAR(36) NOT NULL,
    event_id BIGINT NOT NULL,
    contact_id BIGINT NOT NULL,
    participant_type VARCHAR(32) NOT NULL,
    attendance_status VARCHAR(32) NOT NULL,
    invited_at DATETIME(6) NULL,
    responded_at DATETIME(6) NULL,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_participants_uuid (uuid),
    UNIQUE KEY uk_participants_pair (event_id, contact_id),
    CONSTRAINT fk_participants_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_participants_contact FOREIGN KEY (contact_id) REFERENCES contacts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
