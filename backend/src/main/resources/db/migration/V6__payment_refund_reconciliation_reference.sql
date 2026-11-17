-- Keep manual reconciliation references separate from provider-issued refund IDs.
ALTER TABLE payment_refunds
    ADD COLUMN reconciliation_reference VARCHAR(150) NULL AFTER provider_refund_id,
    ADD UNIQUE KEY uk_payment_refund_reconciliation_reference (reconciliation_reference);
