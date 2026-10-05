-- ============================================================
-- 硬件侠平台 - 商品表字段放宽为可空（支持草稿模式）
-- 迁移时间: 2026-09-07
-- 关联 BUG: bug-20260907163409.md
-- 说明: 草稿模式允许 category_id/price/condition_level 为 null，
--       用户保存草稿时无需填写完整信息
-- 执行方式: mysql -u root -p123456 db_product < migration-product-20260907-draft-nullable.sql
-- ============================================================

USE `db_product`;

-- 分类ID：草稿模式允许为空（用户可能在第2步保存草稿时未选分类）
ALTER TABLE `products`
  MODIFY COLUMN `category_id` INT NULL COMMENT '商品分类（草稿模式可为空）';

-- 售价：草稿模式允许为空
ALTER TABLE `products`
  MODIFY COLUMN `price` DECIMAL(10,2) NULL COMMENT '售价（草稿模式可为空）';

-- 成色：草稿模式允许为空
ALTER TABLE `products`
  MODIFY COLUMN `condition_level` TINYINT NULL COMMENT '成色（草稿模式可为空）';

-- 验证
SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'db_product'
  AND TABLE_NAME = 'products'
  AND COLUMN_NAME IN ('category_id','price','condition_level');
