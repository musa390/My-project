-- Phase 10: extend the existing V1 payments table; never create a replacement payments table.
-- All additions are nullable/defaulted so existing payment rows remain valid.
ALTER TABLE payments
    ADD COLUMN merchant_reference VARCHAR(80) NULL AFTER provider_reference,
    ADD COLUMN idempotency_key VARCHAR(100) NULL AFTER merchant_reference,
    ADD COLUMN verified_at DATETIME NULL AFTER completed_at,
    ADD COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER updated_at,
    ADD UNIQUE KEY uk_payments_idempotency_key (idempotency_key),
    ADD KEY idx_payments_provider_reference (provider, provider_reference);

CREATE TABLE payment_refunds (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT UNSIGNED NOT NULL,
    refund_reference VARCHAR(80) NOT NULL,
    provider_refund_id VARCHAR(150) NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BDT',
    refund_status VARCHAR(24) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(500) NULL,
    requested_by BIGINT UNSIGNED NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payment_refund_reference (refund_reference),
    UNIQUE KEY uk_payment_refund_idempotency (idempotency_key),
    UNIQUE KEY uk_payment_provider_refund (provider_refund_id),
    KEY idx_payment_refunds_payment_date (payment_id, requested_at),
    KEY idx_payment_refunds_status_date (refund_status, requested_at),
    CONSTRAINT fk_payment_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_refunds_actor FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_refund_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_refund_status CHECK (refund_status IN ('REQUESTED','PENDING','SUCCEEDED','FAILED','CANCELLED','MANUAL_REVIEW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payment_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT UNSIGNED NOT NULL,
    provider VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(150) NULL,
    event_idempotency_key VARCHAR(160) NOT NULL,
    verification_status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
    payload_sha256 CHAR(64) NULL,
    safe_payload JSON NULL,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    processing_note VARCHAR(500) NULL,
    UNIQUE KEY uk_payment_event_idempotency (event_idempotency_key),
    UNIQUE KEY uk_payment_provider_event (provider, provider_event_id),
    KEY idx_payment_events_payment_date (payment_id, received_at),
    KEY idx_payment_events_verification (verification_status, received_at),
    CONSTRAINT fk_payment_events_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payment_event_verification CHECK (verification_status IN ('RECEIVED','VERIFIED','REJECTED','DUPLICATE','ERROR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
