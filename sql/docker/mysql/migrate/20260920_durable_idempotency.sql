-- Apply before deploying clients that send Idempotency-Key.
-- Retained until a separately designed retention policy; do not TTL-delete blindly.
CREATE TABLE IF NOT EXISTS request_idempotency (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    operation VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    response_json TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, book_id, operation, request_key),
    KEY idx_idempotency_created_at (created_at)
) ENGINE=InnoDB;
