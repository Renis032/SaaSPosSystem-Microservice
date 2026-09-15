ALTER TABLE orders
    ADD COLUMN branch_id BIGINT NULL,
    ADD COLUMN tax_rate DOUBLE NULL,
    ADD COLUMN tax_amount DOUBLE NULL,
    ADD COLUMN order_discount_percent DOUBLE NULL;

ALTER TABLE shift_report_entity
    ADD COLUMN branch_id BIGINT NULL;
