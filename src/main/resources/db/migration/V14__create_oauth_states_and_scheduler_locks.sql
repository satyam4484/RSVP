CREATE TABLE oauth_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    state CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_states_state (state),
    CONSTRAINT fk_oauth_states_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scheduler_locks (
    name VARCHAR(100) NOT NULL,
    locked_until DATETIME(6) NOT NULL,
    locked_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
