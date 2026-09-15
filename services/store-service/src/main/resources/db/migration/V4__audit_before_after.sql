ALTER TABLE audit_log
    ADD COLUMN before_state VARCHAR(2000) NULL,
    ADD COLUMN after_state VARCHAR(2000) NULL;
