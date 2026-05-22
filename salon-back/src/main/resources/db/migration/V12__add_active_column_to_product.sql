-- Add 'active' column to tb_product table
-- Products created before this migration will be set as active by default
ALTER TABLE tb_product
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
