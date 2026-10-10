CREATE TABLE reminders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid CHAR(36) NOT NULL,
    task_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    reminder_at DATETIME(6) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sent_at DATETIME(6) NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reminders_uuid (uuid),
    CONSTRAINT fk_reminders_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_reminders_event FOREIGN KEY (event_id) REFERENCES events (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    reminder_id BIGINT NULL,
    event_id BIGINT NULL,
    task_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(2000) NULL,
    channel VARCHAR(32) NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notifications_uuid (uuid),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_reminder FOREIGN KEY (reminder_id) REFERENCES reminders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
