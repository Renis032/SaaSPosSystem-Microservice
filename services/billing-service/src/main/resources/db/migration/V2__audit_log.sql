CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NULL,
    actor_user_id BIGINT NULL,
    actor_email VARCHAR(255) NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NULL,
    details VARCHAR(2000) NULL,
    created_at DATETIME(6) NULL
);

CREATE INDEX idx_audit_log_store_created ON audit_log (store_id, created_at);
