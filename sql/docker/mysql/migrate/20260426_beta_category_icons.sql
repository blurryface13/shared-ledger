-- Beta category refresh: keep existing ids, update system category names/icons/sort order.
-- This migration is safe to run more than once.

UPDATE `tb_book_category`
SET `name` = '生鲜日用', `icon` = 'goods', `sort_order` = 3
WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '生活日用';

UPDATE `tb_book_category`
SET `icon` = 'heart', `sort_order` = 9
WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '宠物';

UPDATE `tb_book_category`
SET `name` = '住宿', `icon` = 'home1', `sort_order` = 6
WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '酒店';

UPDATE `tb_book_category`
SET `name` = '通讯', `icon` = 'phone', `sort_order` = 10
WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '公益';

INSERT INTO `tb_book_category` (`book_id`, `name`, `icon`, `category_type`, `category_source`, `status`, `sort_order`, `created_by_user_id`)
SELECT NULL, '通讯', 'phone', 'EXPENSE', 'SYSTEM', 'ACTIVE', 10, `id`
FROM `tb_user`
WHERE `wechat_open_id` = 'system-category-user'
  AND NOT EXISTS (
      SELECT 1 FROM `tb_book_category`
      WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '通讯'
  );

INSERT INTO `tb_book_category` (`book_id`, `name`, `icon`, `category_type`, `category_source`, `status`, `sort_order`, `created_by_user_id`)
SELECT NULL, '数码家电', 'mobile', 'EXPENSE', 'SYSTEM', 'ACTIVE', 15, `id`
FROM `tb_user`
WHERE `wechat_open_id` = 'system-category-user'
  AND NOT EXISTS (
      SELECT 1 FROM `tb_book_category`
      WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '数码家电'
  );

INSERT INTO `tb_book_category` (`book_id`, `name`, `icon`, `category_type`, `category_source`, `status`, `sort_order`, `created_by_user_id`)
SELECT NULL, '旅游', 'location', 'EXPENSE', 'SYSTEM', 'ACTIVE', 16, `id`
FROM `tb_user`
WHERE `wechat_open_id` = 'system-category-user'
  AND NOT EXISTS (
      SELECT 1 FROM `tb_book_category`
      WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '旅游'
  );

INSERT INTO `tb_book_category` (`book_id`, `name`, `icon`, `category_type`, `category_source`, `status`, `sort_order`, `created_by_user_id`)
SELECT NULL, '摄影影音', 'camera', 'EXPENSE', 'SYSTEM', 'ACTIVE', 17, `id`
FROM `tb_user`
WHERE `wechat_open_id` = 'system-category-user'
  AND NOT EXISTS (
      SELECT 1 FROM `tb_book_category`
      WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '摄影影音'
  );

UPDATE `tb_book_category`
SET `icon` = 'wallet', `sort_order` = 21
WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'EXPENSE' AND `name` = '手续费';

UPDATE `tb_book_category`
SET `icon` = 'home'
WHERE `book_id` IS NULL AND `category_source` = 'SYSTEM' AND `category_type` = 'INCOME' AND `name` = '社保公积金';
