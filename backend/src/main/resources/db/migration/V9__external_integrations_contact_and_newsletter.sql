-- Phase 12: contact intake and double-opt-in newsletter tokens.
CREATE TABLE contact_messages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    subject VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    source_ip VARCHAR(45),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_contact_status_created(status, created_at),
    KEY idx_contact_email_created(email, created_at),
    CONSTRAINT chk_contact_status CHECK(status IN ('NEW','IN_PROGRESS','RESOLVED','SPAM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE newsletter_subscribers
    ADD COLUMN confirmation_token_hash CHAR(64) NULL,
    ADD COLUMN confirmation_expires_at DATETIME NULL,
    ADD COLUMN unsubscribe_token_hash CHAR(64) NULL,
    ADD COLUMN consent_source VARCHAR(80) NULL,
    ADD UNIQUE KEY uq_newsletter_confirmation_token(confirmation_token_hash),
    ADD UNIQUE KEY uq_newsletter_unsubscribe_token(unsubscribe_token_hash);
