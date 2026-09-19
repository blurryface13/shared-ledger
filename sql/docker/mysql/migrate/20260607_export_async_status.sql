ALTER TABLE `tb_export_record`
    ADD COLUMN `export_status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '导出状态：PENDING / RUNNING / SUCCESS / FAILED' AFTER `export_type`,
    ADD COLUMN `error_message` VARCHAR(512) NULL COMMENT '失败原因' AFTER `file_url`,
    ADD COLUMN `started_at` DATETIME(3) NULL COMMENT '任务开始时间' AFTER `created_at`,
    ADD COLUMN `finished_at` DATETIME(3) NULL COMMENT '任务结束时间' AFTER `started_at`,
    ADD KEY `idx_export_record_status_created_at` (`export_status`, `created_at`);
