-- Additive migration. Run against an existing database once before enabling travel APIs.
CREATE TABLE IF NOT EXISTS tb_trip (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 owner_id BIGINT NOT NULL,
 book_id BIGINT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 payload JSON NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_trip_book(book_id),
 KEY idx_trip_owner_updated(owner_id,updated_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tb_trip_plan (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 owner_id BIGINT NOT NULL,
 trip_id BIGINT NOT NULL,
 base_version BIGINT NOT NULL,
 payload JSON NOT NULL,
 applied TINYINT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_trip_plan_owner(owner_id,trip_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
