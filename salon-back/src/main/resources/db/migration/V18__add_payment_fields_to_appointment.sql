ALTER TABLE tb_appointment
    ADD COLUMN payment_status VARCHAR(20) DEFAULT 'PAID',
    ADD COLUMN payment_id BIGINT,
    ADD COLUMN pix_qr_code TEXT;

CREATE INDEX IF NOT EXISTS idx_appointment_payment_id ON tb_appointment (payment_id);