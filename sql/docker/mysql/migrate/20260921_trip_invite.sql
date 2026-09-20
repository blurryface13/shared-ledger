CREATE TABLE IF NOT EXISTS tb_trip_invite (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 trip_id BIGINT NOT NULL, target_user_id BIGINT NOT NULL, role VARCHAR(16) NOT NULL,
 token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 expires_at DATETIME NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_trip_invite_token(token_hash), KEY idx_trip_invite_target(trip_id,target_user_id,status)
) ENGINE=InnoDB;
