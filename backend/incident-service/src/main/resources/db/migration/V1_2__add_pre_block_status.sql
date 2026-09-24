-- Flyway Migration V1_2 : Add pre_block_status column to incidents table if missing

ALTER TABLE incidents ADD COLUMN IF NOT EXISTS pre_block_status VARCHAR(255);
