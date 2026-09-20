CREATE TABLE IF NOT EXISTS export_outbox (
 export_record_id BIGINT NOT NULL PRIMARY KEY,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 attempts INT NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 sent_at TIMESTAMP(6) NULL,
 last_error VARCHAR(128) NULL,
 KEY idx_outbox_poll(status, next_attempt_at, export_record_id)
) ENGINE=InnoDB;
