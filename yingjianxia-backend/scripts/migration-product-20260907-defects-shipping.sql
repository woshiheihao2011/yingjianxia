-- ============================================================
-- 硬件侠平台 - 商品表新增字段迁移脚本
-- 迁移时间: 2026-09-07
-- 关联 BUG: bug-20260907095336.md BUG-001
-- 说明: 为商品表新增瑕疵/运费/质保/售后/服务标签字段
-- 执行方式: mysql -u root -p db_product < migration-product-20260907-defects-shipping.sql
-- ============================================================

USE `db_product`;

-- 瑕疵列表（JSON 数组，格式：[{"area":"外观","detail":"右下角轻微磕碰"}]）
ALTER TABLE `products`
  ADD COLUMN `defects_json` TEXT NULL COMMENT '瑕疵列表（JSON）' AFTER `sku`;

-- 是否包邮
ALTER TABLE `products`
  ADD COLUMN `ship_free` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包邮 0否 1是' AFTER `defects_json`;

-- 运费模板（描述性字符串）
ALTER TABLE `products`
  ADD COLUMN `ship_template` VARCHAR(100) NULL COMMENT '运费模板描述' AFTER `ship_free`;

-- 质保说明
ALTER TABLE `products`
  ADD COLUMN `warranty` VARCHAR(50) NULL COMMENT '质保说明' AFTER `ship_template`;

-- 售后类型
ALTER TABLE `products`
  ADD COLUMN `aftersales_type` VARCHAR(20) NULL COMMENT '售后类型 7-days/15-days/none' AFTER `warranty`;

-- 服务承诺标签（JSON 数组，格式：["支持担保交易","7天无理由"]）
ALTER TABLE `products`
  ADD COLUMN `service_tags_json` TEXT NULL COMMENT '服务承诺标签（JSON）' AFTER `aftersales_type`;

-- 验证
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'db_product'
  AND TABLE_NAME = 'products'
  AND COLUMN_NAME IN ('defects_json','ship_free','ship_template','warranty','aftersales_type','service_tags_json');
