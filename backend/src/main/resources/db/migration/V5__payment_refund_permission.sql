-- Phase 10: explicit least-privilege permission for refund operations.
INSERT INTO permissions(code,name,description)
VALUES('payments.refund','Request and reconcile payment refunds','Manage auditable refund review and reconciliation')
ON DUPLICATE KEY UPDATE name=VALUES(name),description=VALUES(description);

INSERT IGNORE INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN','MANAGER') AND p.code='payments.refund';
