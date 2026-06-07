USE `db_trip_ledger`;
SET NAMES utf8mb4;

-- ================================================
-- tb_bill: 共享账单变更链路字段
-- ================================================
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill' AND COLUMN_NAME = 'change_flow_status'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill ADD COLUMN change_flow_status VARCHAR(32) NOT NULL DEFAULT ''NONE'' COMMENT ''共享账单变更状态：NONE / PENDING / HISTORY'' AFTER remark'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill' AND COLUMN_NAME = 'latest_change_request_id'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill ADD COLUMN latest_change_request_id BIGINT NULL COMMENT ''最近一次变更申请 ID（包含待审批/已处理）'' AFTER change_flow_status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill' AND COLUMN_NAME = 'has_change_history'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill ADD COLUMN has_change_history TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否存在变更历史：0 否 / 1 是'' AFTER latest_change_request_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill' AND INDEX_NAME = 'idx_bill_latest_change_request_id'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill ADD INDEX idx_bill_latest_change_request_id (latest_change_request_id)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ================================================
-- tb_bill_change_request: 去 JSON 载荷，新增链路字段
-- ================================================
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill_change_request' AND COLUMN_NAME = 'predecessor_request_id'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill_change_request ADD COLUMN predecessor_request_id BIGINT NULL COMMENT ''前序申请 ID，形成变更链路'' AFTER bill_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill_change_request' AND COLUMN_NAME = 'is_baseline'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill_change_request ADD COLUMN is_baseline TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否为账单基线快照记录：0 否 / 1 是'' AFTER predecessor_request_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill_change_request' AND COLUMN_NAME = 'request_payload_json'
  ),
  'ALTER TABLE tb_bill_change_request DROP COLUMN request_payload_json',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill_change_request' AND INDEX_NAME = 'idx_bill_change_request_predecessor'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill_change_request ADD INDEX idx_bill_change_request_predecessor (predecessor_request_id)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill_change_request' AND CONSTRAINT_NAME = 'fk_bill_change_request_predecessor'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill_change_request ADD CONSTRAINT fk_bill_change_request_predecessor FOREIGN KEY (predecessor_request_id) REFERENCES tb_bill_change_request(id)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ================================================
-- 账单修改快照三张表
-- ================================================
CREATE TABLE IF NOT EXISTS `tb_bill_change_request_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '快照主键',
    `request_id` BIGINT NOT NULL COMMENT '所属申请',
    `bill_type` VARCHAR(32) NOT NULL COMMENT '账单类型',
    `title` VARCHAR(128) NOT NULL COMMENT '账单标题',
    `bill_amount_cent` BIGINT NOT NULL COMMENT '账单总金额，单位分',
    `category_id` BIGINT NULL COMMENT '分类 ID',
    `payer_member_id` BIGINT NOT NULL COMMENT '付款人',
    `recorder_member_id` BIGINT NOT NULL COMMENT '记账人',
    `temp_participant_id` BIGINT NULL COMMENT '个人帮带目标临时成员',
    `bill_time` DATETIME(3) NOT NULL COMMENT '账单发生时间',
    `remark` VARCHAR(1000) NULL COMMENT '备注',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bill_change_request_snapshot_request_id` (`request_id`),
    CONSTRAINT `fk_bill_change_request_snapshot_request_id` FOREIGN KEY (`request_id`) REFERENCES `tb_bill_change_request` (`id`),
    CONSTRAINT `fk_bill_change_request_snapshot_category_id` FOREIGN KEY (`category_id`) REFERENCES `tb_book_category` (`id`),
    CONSTRAINT `fk_bill_change_request_snapshot_payer_member_id` FOREIGN KEY (`payer_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_bill_change_request_snapshot_recorder_member_id` FOREIGN KEY (`recorder_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_bill_change_request_snapshot_temp_participant_id` FOREIGN KEY (`temp_participant_id`) REFERENCES `tb_temp_participant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单申请快照主表';

CREATE TABLE IF NOT EXISTS `tb_bill_change_request_share_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '快照分摊项主键',
    `request_id` BIGINT NOT NULL COMMENT '所属申请',
    `participant_type` VARCHAR(32) NOT NULL COMMENT '分摊对象类型：MEMBER / TEMP_PARTICIPANT',
    `participant_ref_id` BIGINT NOT NULL COMMENT '分摊对象 ID',
    `attached_member_id` BIGINT NULL COMMENT '若分摊对象是临时成员，则记录挂靠成员快照',
    `share_method` VARCHAR(32) NOT NULL COMMENT '分摊方式：AVERAGE / FIXED_AMOUNT / RATIO',
    `share_ratio` DECIMAL(10,4) NULL COMMENT '比例值',
    `share_amount_cent` BIGINT NOT NULL COMMENT '分摊金额，单位分',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_bill_change_request_share_item_request_id` (`request_id`),
    KEY `idx_bill_change_request_share_item_participant` (`participant_type`, `participant_ref_id`),
    KEY `idx_bill_change_request_share_item_attached_member_id` (`attached_member_id`),
    CONSTRAINT `fk_bill_change_request_share_item_request_id` FOREIGN KEY (`request_id`) REFERENCES `tb_bill_change_request` (`id`),
    CONSTRAINT `fk_bill_change_request_share_item_attached_member_id` FOREIGN KEY (`attached_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单申请快照分摊项表';

CREATE TABLE IF NOT EXISTS `tb_bill_change_request_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '快照附件主键',
    `request_id` BIGINT NOT NULL COMMENT '所属申请',
    `file_url` VARCHAR(512) NOT NULL COMMENT '文件地址',
    `file_type` VARCHAR(32) NOT NULL COMMENT '文件类型：IMAGE / OTHER',
    `uploaded_by_member_id` BIGINT NOT NULL COMMENT '上传人',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_bill_change_request_attachment_request_id` (`request_id`),
    CONSTRAINT `fk_bill_change_request_attachment_request_id` FOREIGN KEY (`request_id`) REFERENCES `tb_bill_change_request` (`id`),
    CONSTRAINT `fk_bill_change_request_attachment_uploaded_by_member_id` FOREIGN KEY (`uploaded_by_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单申请快照附件表';

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_bill' AND CONSTRAINT_NAME = 'fk_bill_latest_change_request_id'
  ),
  'SELECT 1',
  'ALTER TABLE tb_bill ADD CONSTRAINT fk_bill_latest_change_request_id FOREIGN KEY (latest_change_request_id) REFERENCES tb_bill_change_request(id)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ================================================
-- 去 JSON 列类型（保留列名）
-- ================================================
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_settlement_transfer' AND COLUMN_NAME = 'related_summary_json' AND DATA_TYPE = 'json'
  ),
  'ALTER TABLE tb_settlement_transfer MODIFY COLUMN related_summary_json VARCHAR(2000) NULL COMMENT ''转账构成摘要（文本）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_operation_log' AND COLUMN_NAME = 'before_json' AND DATA_TYPE = 'json'
  ),
  'ALTER TABLE tb_operation_log MODIFY COLUMN before_json VARCHAR(2000) NULL COMMENT ''操作前快照文本''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_operation_log' AND COLUMN_NAME = 'after_json' AND DATA_TYPE = 'json'
  ),
  'ALTER TABLE tb_operation_log MODIFY COLUMN after_json VARCHAR(2000) NULL COMMENT ''操作后快照文本''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ================================================
-- 账单链路状态回填
-- ================================================
UPDATE tb_bill b
LEFT JOIN (
    SELECT bill_id, MAX(id) AS latest_id
    FROM tb_bill_change_request
    GROUP BY bill_id
) r ON r.bill_id = b.id
SET b.latest_change_request_id = r.latest_id;

UPDATE tb_bill b
SET b.has_change_history = CASE
    WHEN EXISTS (SELECT 1 FROM tb_bill_change_request r WHERE r.bill_id = b.id) THEN 1
    ELSE 0
END;

UPDATE tb_bill b
SET b.change_flow_status = CASE
    WHEN EXISTS (
        SELECT 1 FROM tb_bill_change_request r
        WHERE r.bill_id = b.id AND r.status = 'PENDING'
    ) THEN 'PENDING'
    WHEN b.has_change_history = 1 THEN 'HISTORY'
    ELSE 'NONE'
END;
