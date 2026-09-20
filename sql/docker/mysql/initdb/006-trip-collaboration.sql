CREATE TABLE IF NOT EXISTS tb_trip_member (
 trip_id BIGINT NOT NULL, user_id BIGINT NOT NULL, role VARCHAR(16) NOT NULL,
 PRIMARY KEY(trip_id,user_id), KEY idx_trip_member_user(user_id,trip_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS tb_trip_change (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, trip_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL, version BIGINT NOT NULL, action VARCHAR(96) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_trip_change_cursor(trip_id,id)
) ENGINE=InnoDB;
