CREATE TABLE user_notifications (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    event_key VARCHAR(191) NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id BIGINT UNSIGNED NOT NULL,
    title_key VARCHAR(120) NOT NULL,
    message_key VARCHAR(160) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_notifications_event (user_id, event_key),
    KEY idx_user_notifications_inbox (user_id, is_read, created_at, id),
    CONSTRAINT fk_user_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
