CREATE DATABASE IF NOT EXISTS `db_trip_ledger`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `db_trip_ledger`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 共享记账本 V1 - MySQL 8.4 建表脚本
-- =========================================================

-- =========================================================
-- 1. user 用户表
-- =========================================================
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `wechat_open_id` VARCHAR(64) NOT NULL COMMENT '微信 openId',
    `wechat_union_id` VARCHAR(64) NULL COMMENT '微信 unionId',
    `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(512) NULL COMMENT '头像地址',
    `mobile` VARCHAR(32) NULL COMMENT '手机号',
    `status` VARCHAR(32) NOT NULL COMMENT '用户状态：ACTIVE / DISABLED / CANCELLED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_wechat_open_id` (`wechat_open_id`),
    UNIQUE KEY `uk_user_wechat_union_id` (`wechat_union_id`),
    UNIQUE KEY `uk_user_mobile` (`mobile`),
    KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

INSERT INTO `tb_user` (`wechat_open_id`, `nickname`, `avatar_url`, `mobile`, `status`)
VALUES ('system-category-user', '系统分类', NULL, NULL, 'DISABLED');

-- =========================================================
-- 2. book 账本表
-- =========================================================
DROP TABLE IF EXISTS `tb_book`;
CREATE TABLE `tb_book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账本主键',
    `name` VARCHAR(128) NOT NULL COMMENT '账本名称',
    `book_type` VARCHAR(32) NOT NULL COMMENT '账本类型：PERSONAL / SHARED',
    `owner_user_id` BIGINT NOT NULL COMMENT '创建者用户 ID，外键来自db_user:id',
    `description` VARCHAR(512) NULL COMMENT '账本描述',
    `cover_url` VARCHAR(512) NULL COMMENT '封面图',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '账本状态：ACTIVE / DELETED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_owner_name` (`owner_user_id`, `name`),
    KEY `idx_book_owner_user_id` (`owner_user_id`),
    KEY `idx_book_type_status` (`book_type`, `status`),
    CONSTRAINT `fk_book_owner_user_id` FOREIGN KEY (`owner_user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账本表';

-- =========================================================
-- 3. book_member 账本成员表
-- =========================================================
DROP TABLE IF EXISTS `tb_book_member`;
CREATE TABLE `tb_book_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员关系主键',
    `book_id` BIGINT NOT NULL COMMENT '所属账本',
    `user_id` BIGINT NOT NULL COMMENT '对应用户',
    `budget_amount_cent` BIGINT NOT NULL DEFAULT 0 COMMENT '该成员在当前账本设置的旅行预算（单位分）',
    `member_role` VARCHAR(32) NOT NULL COMMENT '成员角色：OWNER / ADMIN / MEMBER',
    `member_status` VARCHAR(32) NOT NULL COMMENT '成员状态：ACTIVE / QUIT / REMOVED',
    `joined_at` DATETIME(3) NOT NULL COMMENT '最近一次加入时间',
    `left_at` DATETIME(3) NULL COMMENT '最近一次退出/移除时间',
    `invited_by_user_id` BIGINT NOT NULL COMMENT '邀请人',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_member_book_user` (`book_id`, `user_id`),
    KEY `idx_book_member_book_status` (`book_id`, `member_status`),
    KEY `idx_book_member_user_status` (`user_id`, `member_status`),
    CONSTRAINT `fk_book_member_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_book_member_user_id` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`),
    CONSTRAINT `fk_book_member_invited_by_user_id` FOREIGN KEY (`invited_by_user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账本成员表';

-- =========================================================
-- 4. book_invitation 账本邀请表
-- =========================================================
DROP TABLE IF EXISTS `tb_book_invitation`;
CREATE TABLE `tb_book_invitation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请记录主键',
    `book_id` BIGINT NOT NULL COMMENT '所属账本',
    `inviter_user_id` BIGINT NOT NULL COMMENT '邀请人',
    `invitee_user_id` BIGINT NOT NULL COMMENT '被邀请人',
    `status` VARCHAR(32) NOT NULL COMMENT '邀请状态：PENDING / ACCEPTED / REJECTED / EXPIRED',
    `invited_at` DATETIME(3) NOT NULL COMMENT '邀请时间',
    `handled_at` DATETIME(3) NULL COMMENT '处理时间',
    `remark` VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_book_invitation_book_id` (`book_id`),
    KEY `idx_book_invitation_invitee_status` (`invitee_user_id`, `status`),
    CONSTRAINT `fk_book_invitation_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_book_invitation_inviter_user_id` FOREIGN KEY (`inviter_user_id`) REFERENCES `tb_user` (`id`),
    CONSTRAINT `fk_book_invitation_invitee_user_id` FOREIGN KEY (`invitee_user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账本邀请表';

-- =========================================================
-- 5. book_category 账本分类表
-- =========================================================
DROP TABLE IF EXISTS `tb_book_category`;
CREATE TABLE `tb_book_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类主键',
    `book_id` BIGINT NULL COMMENT '所属账本，SYSTEM 为空，CUSTOM_GLOBAL/CUSTOM_LOCAL 必填',
    `name` VARCHAR(64) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(512) NULL COMMENT '分类图标标识，推荐存 Wot UI icon name 或图片 URL',
    `category_type` VARCHAR(32) NOT NULL COMMENT '分类类型：EXPENSE / INCOME',
    `category_source` VARCHAR(32) NOT NULL COMMENT '分类来源：SYSTEM / CUSTOM_GLOBAL / CUSTOM_LOCAL',
    `status` VARCHAR(32) NOT NULL COMMENT '分类状态：ACTIVE / DISABLED',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `created_by_user_id` BIGINT NOT NULL COMMENT '创建人',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_category_book_status` (`book_id`, `status`),
    KEY `idx_book_category_book_sort` (`book_id`, `sort_order`),
    KEY `idx_book_category_creator_source` (`created_by_user_id`, `category_source`, `status`),
    KEY `idx_book_category_type_status` (`category_type`, `status`),
    CONSTRAINT `fk_book_category_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_book_category_created_by_user_id` FOREIGN KEY (`created_by_user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账本分类表';

INSERT INTO `tb_book_category` (`book_id`, `name`, `icon`, `category_type`, `category_source`, `status`, `sort_order`, `created_by_user_id`)
VALUES
    (NULL, '餐饮', 'shop', 'EXPENSE', 'SYSTEM', 'ACTIVE', 1, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '服饰美容', 'bags', 'EXPENSE', 'SYSTEM', 'ACTIVE', 2, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '生鲜日用', 'goods', 'EXPENSE', 'SYSTEM', 'ACTIVE', 3, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '购物', 'cart', 'EXPENSE', 'SYSTEM', 'ACTIVE', 4, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '出行', 'car', 'EXPENSE', 'SYSTEM', 'ACTIVE', 5, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '住宿', 'home1', 'EXPENSE', 'SYSTEM', 'ACTIVE', 6, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '休闲娱乐', 'play-circle', 'EXPENSE', 'SYSTEM', 'ACTIVE', 7, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '文体教育', 'books', 'EXPENSE', 'SYSTEM', 'ACTIVE', 8, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '宠物', 'heart', 'EXPENSE', 'SYSTEM', 'ACTIVE', 9, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '通讯', 'phone', 'EXPENSE', 'SYSTEM', 'ACTIVE', 10, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '医疗保健', 'service', 'EXPENSE', 'SYSTEM', 'ACTIVE', 11, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '充值缴费', 'creditcard', 'EXPENSE', 'SYSTEM', 'ACTIVE', 12, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '房租房贷', 'home1', 'EXPENSE', 'SYSTEM', 'ACTIVE', 13, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '保险', 'secured', 'EXPENSE', 'SYSTEM', 'ACTIVE', 14, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '数码家电', 'mobile', 'EXPENSE', 'SYSTEM', 'ACTIVE', 15, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '旅游', 'location', 'EXPENSE', 'SYSTEM', 'ACTIVE', 16, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '摄影影音', 'camera', 'EXPENSE', 'SYSTEM', 'ACTIVE', 17, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '现金', 'money-circle', 'EXPENSE', 'SYSTEM', 'ACTIVE', 18, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '转账给他人', 'transfer', 'EXPENSE', 'SYSTEM', 'ACTIVE', 19, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '红包', 'gift', 'EXPENSE', 'SYSTEM', 'ACTIVE', 20, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '手续费', 'wallet', 'EXPENSE', 'SYSTEM', 'ACTIVE', 21, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '还款', 'wallet', 'EXPENSE', 'SYSTEM', 'ACTIVE', 22, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '其他支出', 'more1', 'EXPENSE', 'SYSTEM', 'ACTIVE', 23, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '薪酬', 'money-circle', 'INCOME', 'SYSTEM', 'ACTIVE', 1, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '借款', 'wallet', 'INCOME', 'SYSTEM', 'ACTIVE', 2, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '报销', 'creditcard', 'INCOME', 'SYSTEM', 'ACTIVE', 3, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '退款', 'refresh', 'INCOME', 'SYSTEM', 'ACTIVE', 4, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '投资收益', 'chart-pie', 'INCOME', 'SYSTEM', 'ACTIVE', 5, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '社保公积金', 'home', 'INCOME', 'SYSTEM', 'ACTIVE', 6, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '他人转入', 'transfer', 'INCOME', 'SYSTEM', 'ACTIVE', 7, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '现金', 'money-circle', 'INCOME', 'SYSTEM', 'ACTIVE', 8, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '红包', 'gift', 'INCOME', 'SYSTEM', 'ACTIVE', 9, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user')),
    (NULL, '其他收入', 'more1', 'INCOME', 'SYSTEM', 'ACTIVE', 10, (SELECT `id` FROM `tb_user` WHERE `wechat_open_id` = 'system-category-user'));

-- =========================================================
-- 6. temp_participant 临时成员表
-- =========================================================
DROP TABLE IF EXISTS `tb_temp_participant`;
CREATE TABLE `tb_temp_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '临时成员主键',
    `book_id` BIGINT NOT NULL COMMENT '所属账本',
    `nickname` VARCHAR(64) NOT NULL COMMENT '临时成员昵称',
    `temp_type` VARCHAR(32) NOT NULL COMMENT '临时成员类型：PRIVATE / GLOBAL',
    `created_by_member_id` BIGINT NOT NULL COMMENT '创建该临时成员的正式成员',
    `attached_member_id` BIGINT NOT NULL COMMENT '当前挂靠正式成员',
    `status` VARCHAR(32) NOT NULL COMMENT '状态：ACTIVE / DISABLED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_temp_participant_book_type` (`book_id`, `temp_type`),
    KEY `idx_temp_participant_book_type_status` (`book_id`, `temp_type`, `status`),
    KEY `idx_temp_participant_attached_member_id` (`attached_member_id`),
    KEY `idx_temp_participant_created_by_member_id` (`created_by_member_id`),
    CONSTRAINT `fk_temp_participant_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_temp_participant_created_by_member_id` FOREIGN KEY (`created_by_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_temp_participant_attached_member_id` FOREIGN KEY (`attached_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='临时成员表';

-- =========================================================
-- 7. bill 账单主表
-- =========================================================
DROP TABLE IF EXISTS `tb_bill`;
CREATE TABLE `tb_bill` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账单主键',
    `book_id` BIGINT NOT NULL COMMENT '所属账本',
    `bill_type` VARCHAR(32) NOT NULL COMMENT '账单类型：PERSONAL_EXPENSE / PERSONAL_INCOME / PERSONAL_CARRY / SHARED_EXPENSE',
    `title` VARCHAR(128) NOT NULL COMMENT '账单标题',
    `bill_amount_cent` BIGINT NOT NULL COMMENT '账单总金额，单位分',
    `category_id` BIGINT NULL COMMENT '分类 ID',
    `payer_member_id` BIGINT NOT NULL COMMENT '付款人',
    `recorder_member_id` BIGINT NOT NULL COMMENT '记账人',
    `target_temp_participant_id` BIGINT NULL COMMENT '目标临时成员，仅 PERSONAL_CARRY 使用',
    `bill_time` DATETIME(3) NOT NULL COMMENT '账单发生时间',
    `remark` VARCHAR(1000) NULL COMMENT '备注',
    `change_flow_status` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '共享账单变更状态：NONE / PENDING / HISTORY',
    `latest_change_request_id` BIGINT NULL COMMENT '最近一次变更申请 ID（包含待审批/已处理）',
    `has_change_history` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否存在变更历史：0 否 / 1 是',
    `status` VARCHAR(32) NOT NULL COMMENT '账单状态：ACTIVE / DELETED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_bill_book_type_status` (`book_id`, `bill_type`, `status`),
    KEY `idx_bill_payer_member_id` (`payer_member_id`),
    KEY `idx_bill_recorder_member_id` (`recorder_member_id`),
    KEY `idx_bill_target_temp_participant_id` (`target_temp_participant_id`),
    KEY `idx_bill_latest_change_request_id` (`latest_change_request_id`),
    KEY `idx_bill_bill_time` (`bill_time`),
    CONSTRAINT `fk_bill_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_bill_category_id` FOREIGN KEY (`category_id`) REFERENCES `tb_book_category` (`id`),
    CONSTRAINT `fk_bill_payer_member_id` FOREIGN KEY (`payer_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_bill_recorder_member_id` FOREIGN KEY (`recorder_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_bill_target_temp_participant_id` FOREIGN KEY (`target_temp_participant_id`) REFERENCES `tb_temp_participant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单主表';

-- =========================================================
-- 8. bill_attachment 账单附件表
-- =========================================================
DROP TABLE IF EXISTS `tb_bill_attachment`;
CREATE TABLE `tb_bill_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '附件主键',
    `bill_id` BIGINT NOT NULL COMMENT '所属账单',
    `file_url` VARCHAR(512) NOT NULL COMMENT '文件地址',
    `file_type` VARCHAR(32) NOT NULL COMMENT '文件类型：IMAGE / OTHER',
    `uploaded_by_member_id` BIGINT NOT NULL COMMENT '上传人',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_bill_attachment_bill_id` (`bill_id`),
    CONSTRAINT `fk_bill_attachment_bill_id` FOREIGN KEY (`bill_id`) REFERENCES `tb_bill` (`id`),
    CONSTRAINT `fk_bill_attachment_uploaded_by_member_id` FOREIGN KEY (`uploaded_by_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单附件表';

-- =========================================================
-- 9. bill_share_item 账单分摊项表
-- =========================================================
DROP TABLE IF EXISTS `tb_bill_share_item`;
CREATE TABLE `tb_bill_share_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分摊项主键',
    `bill_id` BIGINT NOT NULL COMMENT '所属账单',
    `participant_type` VARCHAR(32) NOT NULL COMMENT '分摊对象类型：MEMBER / TEMP_PARTICIPANT',
    `participant_ref_id` BIGINT NOT NULL COMMENT '分摊对象 ID，指向 book_member.id 或 temp_participant.id',
    `attached_member_id` BIGINT NULL COMMENT '若分摊对象是临时成员，则记录挂靠成员快照',
    `share_method` VARCHAR(32) NOT NULL COMMENT '分摊方式：AVERAGE / FIXED_AMOUNT / RATIO',
    `share_ratio` DECIMAL(10,4) NULL COMMENT '比例值',
    `share_amount_cent` BIGINT NOT NULL COMMENT '分摊金额，单位分',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_bill_share_item_bill_id` (`bill_id`),
    KEY `idx_bill_share_item_participant` (`participant_type`, `participant_ref_id`),
    KEY `idx_bill_share_item_attached_member_id` (`attached_member_id`),
    CONSTRAINT `fk_bill_share_item_bill_id` FOREIGN KEY (`bill_id`) REFERENCES `tb_bill` (`id`),
    CONSTRAINT `fk_bill_share_item_attached_member_id` FOREIGN KEY (`attached_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单分摊项表';

-- =========================================================
-- 10. bill_change_request 账单申请表
-- =========================================================
DROP TABLE IF EXISTS `tb_bill_change_request`;
CREATE TABLE `tb_bill_change_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请主键',
    `bill_id` BIGINT NOT NULL COMMENT '目标账单',
    `predecessor_request_id` BIGINT NULL COMMENT '前序申请 ID，形成变更链路',
    `is_baseline` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为账单基线快照记录：0 否 / 1 是',
    `request_type` VARCHAR(32) NOT NULL COMMENT '申请类型：MODIFY / DELETE',
    `requester_member_id` BIGINT NOT NULL COMMENT '申请人',
    `request_reason` VARCHAR(1000) NULL COMMENT '申请原因',
    `status` VARCHAR(32) NOT NULL COMMENT '申请状态：PENDING / APPROVED / REJECTED / CANCELLED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `handled_at` DATETIME(3) NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_bill_change_request_bill_id` (`bill_id`),
    KEY `idx_bill_change_request_predecessor` (`predecessor_request_id`),
    KEY `idx_bill_change_request_requester_status` (`requester_member_id`, `status`),
    KEY `idx_bill_change_request_type_status` (`request_type`, `status`),
    CONSTRAINT `fk_bill_change_request_bill_id` FOREIGN KEY (`bill_id`) REFERENCES `tb_bill` (`id`),
    CONSTRAINT `fk_bill_change_request_predecessor` FOREIGN KEY (`predecessor_request_id`) REFERENCES `tb_bill_change_request` (`id`),
    CONSTRAINT `fk_bill_change_request_requester_member_id` FOREIGN KEY (`requester_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单申请表';

-- =========================================================
-- 11. bill_change_request_snapshot 账单申请快照主表
-- =========================================================
DROP TABLE IF EXISTS `tb_bill_change_request_snapshot`;
CREATE TABLE `tb_bill_change_request_snapshot` (
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

-- =========================================================
-- 12. bill_change_request_share_item 账单申请快照分摊项表
-- =========================================================
DROP TABLE IF EXISTS `tb_bill_change_request_share_item`;
CREATE TABLE `tb_bill_change_request_share_item` (
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

-- =========================================================
-- 13. bill_change_request_attachment 账单申请快照附件表
-- =========================================================
DROP TABLE IF EXISTS `tb_bill_change_request_attachment`;
CREATE TABLE `tb_bill_change_request_attachment` (
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

-- =========================================================
-- 14. bill 增量外键补充
-- =========================================================
ALTER TABLE `tb_bill`
    ADD CONSTRAINT `fk_bill_latest_change_request_id`
    FOREIGN KEY (`latest_change_request_id`) REFERENCES `tb_bill_change_request` (`id`);

-- =========================================================
-- 15. bill_change_request_approval 申请审批表
-- =========================================================
DROP TABLE IF EXISTS `tb_bill_change_request_approval`;
CREATE TABLE `tb_bill_change_request_approval` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审批记录主键',
    `request_id` BIGINT NOT NULL COMMENT '所属申请',
    `approver_member_id` BIGINT NOT NULL COMMENT '审批人',
    `approval_action` VARCHAR(32) NOT NULL COMMENT '审批动作：APPROVE / REJECT',
    `approval_comment` VARCHAR(1000) NULL COMMENT '审批备注',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '审批时间',
    PRIMARY KEY (`id`),
    KEY `idx_bill_change_request_approval_request_id` (`request_id`),
    KEY `idx_bill_change_request_approval_approver_member_id` (`approver_member_id`),
    CONSTRAINT `fk_bill_change_request_approval_request_id` FOREIGN KEY (`request_id`) REFERENCES `tb_bill_change_request` (`id`),
    CONSTRAINT `fk_bill_change_request_approval_approver_member_id` FOREIGN KEY (`approver_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申请审批表';

-- =========================================================
-- 16. settlement_batch 结算批次表
-- =========================================================
DROP TABLE IF EXISTS `tb_settlement_batch`;
CREATE TABLE `tb_settlement_batch` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '结算批次主键',
    `book_id` BIGINT NOT NULL COMMENT '所属账本',
    `initiator_member_id` BIGINT NOT NULL COMMENT '发起结算成员',
    `strategy_type` VARCHAR(32) NOT NULL COMMENT '结算策略：INTUITIVE_FIRST / MIN_TRANSFER_COUNT',
    `scope_type` VARCHAR(32) NOT NULL COMMENT '视角类型：PERSONAL_VIEW',
    `status` VARCHAR(32) NOT NULL COMMENT '计算状态：SUCCESS / FAILED',
    `snapshot_time` DATETIME(3) NOT NULL COMMENT '快照时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_settlement_batch_book_initiator` (`book_id`, `initiator_member_id`),
    KEY `idx_settlement_batch_book_created_at` (`book_id`, `created_at`),
    CONSTRAINT `fk_settlement_batch_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_settlement_batch_initiator_member_id` FOREIGN KEY (`initiator_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算批次表';

-- =========================================================
-- 17. settlement_transfer 结算建议转账表
-- =========================================================
DROP TABLE IF EXISTS `tb_settlement_transfer`;
CREATE TABLE `tb_settlement_transfer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '建议转账主键',
    `settlement_batch_id` BIGINT NOT NULL COMMENT '所属结算批次',
    `from_member_id` BIGINT NOT NULL COMMENT '建议付款方',
    `to_member_id` BIGINT NOT NULL COMMENT '建议收款方',
    `transfer_amount_cent` BIGINT NOT NULL COMMENT '建议转账金额，单位分',
    `related_summary_json` VARCHAR(2000) NULL COMMENT '转账构成摘要（文本）',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_settlement_transfer_batch_id` (`settlement_batch_id`),
    KEY `idx_settlement_transfer_from_to` (`from_member_id`, `to_member_id`),
    CONSTRAINT `fk_settlement_transfer_batch_id` FOREIGN KEY (`settlement_batch_id`) REFERENCES `tb_settlement_batch` (`id`),
    CONSTRAINT `fk_settlement_transfer_from_member_id` FOREIGN KEY (`from_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_settlement_transfer_to_member_id` FOREIGN KEY (`to_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算建议转账表';

-- =========================================================
-- 18. payment_confirm_record 成员间支付确认表
-- =========================================================
DROP TABLE IF EXISTS `tb_payment_confirm_record`;
CREATE TABLE `tb_payment_confirm_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付确认主键',
    `book_id` BIGINT NOT NULL COMMENT '所属账本',
    `from_member_id` BIGINT NOT NULL COMMENT '付款方',
    `to_member_id` BIGINT NOT NULL COMMENT '收款方',
    `payment_amount_cent` BIGINT NOT NULL COMMENT '支付金额，单位分',
    `source_type` VARCHAR(32) NOT NULL COMMENT '来源类型：SETTLEMENT_TRANSFER / MANUAL',
    `source_ref_id` BIGINT NULL COMMENT '来源业务 ID，如来源于转账建议，那么就存转账主键ID',
    `confirm_status` VARCHAR(32) NOT NULL COMMENT '确认状态：PENDING_CONFIRM / CONFIRMED / AUTO_CONFIRMED / REJECT',
    `initiated_by_member_id` BIGINT NOT NULL COMMENT '发起“已付款”的成员',
    `confirmed_by_member_id` BIGINT NULL COMMENT '确认成员',
    `initiated_at` DATETIME(3) NOT NULL COMMENT '发起时间',
    `confirmed_at` DATETIME(3) NULL COMMENT '确认时间',
    `auto_confirm_at` DATETIME(3) NULL COMMENT '自动确认时间',
    `remark` VARCHAR(1000) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_payment_confirm_record_book_id` (`book_id`),
    KEY `idx_payment_confirm_record_from_status` (`from_member_id`, `confirm_status`),
    KEY `idx_payment_confirm_record_to_status` (`to_member_id`, `confirm_status`),
    KEY `idx_payment_confirm_record_source` (`source_type`, `source_ref_id`),
    CONSTRAINT `fk_payment_confirm_record_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_payment_confirm_record_from_member_id` FOREIGN KEY (`from_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_payment_confirm_record_to_member_id` FOREIGN KEY (`to_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_payment_confirm_record_initiated_by_member_id` FOREIGN KEY (`initiated_by_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_payment_confirm_record_confirmed_by_member_id` FOREIGN KEY (`confirmed_by_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成员间支付确认表';

-- =========================================================
-- 19. payment_confirm_allocation 支付确认账单分配表
-- =========================================================
DROP TABLE IF EXISTS `tb_payment_confirm_allocation`;
CREATE TABLE `tb_payment_confirm_allocation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付分配主键',
    `payment_confirm_id` BIGINT NOT NULL COMMENT '所属支付确认记录',
    `bill_id` BIGINT NOT NULL COMMENT '关联账单',
    `allocated_amount_cent` BIGINT NOT NULL COMMENT '分配到该账单的金额，单位分',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_payment_confirm_allocation_record_id` (`payment_confirm_id`),
    KEY `idx_payment_confirm_allocation_bill_id` (`bill_id`),
    CONSTRAINT `fk_payment_confirm_allocation_record_id` FOREIGN KEY (`payment_confirm_id`) REFERENCES `tb_payment_confirm_record` (`id`),
    CONSTRAINT `fk_payment_confirm_allocation_bill_id` FOREIGN KEY (`bill_id`) REFERENCES `tb_bill` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付确认账单分配表';

-- =========================================================
-- 20. temp_recovery_record 临时成员回补记录表
-- =========================================================
DROP TABLE IF EXISTS `tb_temp_recovery_record`;
CREATE TABLE `tb_temp_recovery_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '回补记录主键',
    `book_id` BIGINT NOT NULL COMMENT '所属账本',
    `temp_participant_id` BIGINT NOT NULL COMMENT '临时成员',
    `attached_member_id` BIGINT NOT NULL COMMENT '挂靠正式成员',
    `recovery_amount_cent` BIGINT NOT NULL COMMENT '回补金额，单位分',
    `confirm_status` VARCHAR(32) NOT NULL COMMENT '确认状态：CONFIRMED',
    `confirmed_by_member_id` BIGINT NOT NULL COMMENT '确认人，应为挂靠正式成员',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `confirmed_at` DATETIME(3) NOT NULL COMMENT '确认时间',
    `remark` VARCHAR(1000) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_temp_recovery_record_temp_participant_id` (`temp_participant_id`),
    KEY `idx_temp_recovery_record_attached_member_id` (`attached_member_id`),
    CONSTRAINT `fk_temp_recovery_record_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_temp_recovery_record_temp_participant_id` FOREIGN KEY (`temp_participant_id`) REFERENCES `tb_temp_participant` (`id`),
    CONSTRAINT `fk_temp_recovery_record_attached_member_id` FOREIGN KEY (`attached_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_temp_recovery_record_confirmed_by_member_id` FOREIGN KEY (`confirmed_by_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='临时成员回补记录表';

-- =========================================================
-- 21. temp_recovery_allocation 临时成员回补账单分配表
-- =========================================================
DROP TABLE IF EXISTS `tb_temp_recovery_allocation`;
CREATE TABLE `tb_temp_recovery_allocation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '回补分配主键',
    `recovery_record_id` BIGINT NOT NULL COMMENT '所属回补记录',
    `bill_id` BIGINT NOT NULL COMMENT '关联账单',
    `allocated_amount_cent` BIGINT NOT NULL COMMENT '分配到该账单的金额，单位分',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_temp_recovery_allocation_record_id` (`recovery_record_id`),
    KEY `idx_temp_recovery_allocation_bill_id` (`bill_id`),
    CONSTRAINT `fk_temp_recovery_allocation_record_id` FOREIGN KEY (`recovery_record_id`) REFERENCES `tb_temp_recovery_record` (`id`),
    CONSTRAINT `fk_temp_recovery_allocation_bill_id` FOREIGN KEY (`bill_id`) REFERENCES `tb_bill` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='临时成员回补账单分配表';

-- =========================================================
-- 22. operation_log 操作日志表
-- =========================================================
DROP TABLE IF EXISTS `tb_operation_log`;
CREATE TABLE `tb_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    `book_id` BIGINT NULL COMMENT '所属账本',
    `operator_member_id` BIGINT NULL COMMENT '操作成员',
    `operator_user_id` BIGINT NULL COMMENT '操作用户',
    `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型',
    `target_type` VARCHAR(64) NOT NULL COMMENT '目标对象类型',
    `target_id` BIGINT NOT NULL COMMENT '目标对象 ID',
    `before_json` VARCHAR(2000) NULL COMMENT '操作前快照文本',
    `after_json` VARCHAR(2000) NULL COMMENT '操作后快照文本',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '日志时间',
    PRIMARY KEY (`id`),
    KEY `idx_operation_log_book_id` (`book_id`),
    KEY `idx_operation_log_operator_member_id` (`operator_member_id`),
    KEY `idx_operation_log_target` (`target_type`, `target_id`),
    KEY `idx_operation_log_created_at` (`created_at`),
    CONSTRAINT `fk_operation_log_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_operation_log_operator_member_id` FOREIGN KEY (`operator_member_id`) REFERENCES `tb_book_member` (`id`),
    CONSTRAINT `fk_operation_log_operator_user_id` FOREIGN KEY (`operator_user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =========================================================
-- 23. export_record 导出记录表
-- =========================================================
DROP TABLE IF EXISTS `tb_export_record`;
CREATE TABLE `tb_export_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '导出记录主键',
    `book_id` BIGINT NOT NULL COMMENT '所属账本',
    `operator_member_id` BIGINT NOT NULL COMMENT '导出发起人',
    `export_type` VARCHAR(32) NOT NULL COMMENT '导出类型：PERSONAL_DETAIL / BOOK_SUMMARY',
    `export_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '导出状态：PENDING / RUNNING / SUCCESS / FAILED',
    `file_url` VARCHAR(512) NULL COMMENT '导出文件地址',
    `error_message` VARCHAR(512) NULL COMMENT '失败原因',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '导出时间',
    `started_at` DATETIME(3) NULL COMMENT '任务开始时间',
    `finished_at` DATETIME(3) NULL COMMENT '任务结束时间',
    PRIMARY KEY (`id`),
    KEY `idx_export_record_book_id` (`book_id`),
    KEY `idx_export_record_operator_member_id` (`operator_member_id`),
    KEY `idx_export_record_export_type` (`export_type`),
    KEY `idx_export_record_status_created_at` (`export_status`, `created_at`),
    CONSTRAINT `fk_export_record_book_id` FOREIGN KEY (`book_id`) REFERENCES `tb_book` (`id`),
    CONSTRAINT `fk_export_record_operator_member_id` FOREIGN KEY (`operator_member_id`) REFERENCES `tb_book_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出记录表';

-- =========================================================
-- 24. user_refresh_token 认证刷新会话表
-- =========================================================
DROP TABLE IF EXISTS `tb_user_refresh_token`;
CREATE TABLE `tb_user_refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'refresh token 会话主键',
    `user_id` BIGINT NOT NULL COMMENT '所属用户 ID',
    `token_hash` VARCHAR(128) NOT NULL COMMENT 'refresh token 摘要值，不保存明文',
    `status` VARCHAR(32) NOT NULL COMMENT '会话状态：ACTIVE / USED / REVOKED / EXPIRED',
    `expire_at` DATETIME(3) NOT NULL COMMENT 'refresh token 过期时间',
    `last_used_at` DATETIME(3) NULL COMMENT '最近一次使用时间',
    `revoked_at` DATETIME(3) NULL COMMENT '撤销时间',
    `replaced_by_token_hash` VARCHAR(128) NULL COMMENT '旋转续签后新 token 的摘要',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_refresh_token_hash` (`token_hash`),
    KEY `idx_user_refresh_token_user_status` (`user_id`, `status`),
    KEY `idx_user_refresh_token_expire_at` (`expire_at`),
    CONSTRAINT `fk_user_refresh_token_user_id` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户 refresh token 会话表';

SET FOREIGN_KEY_CHECKS = 1;
