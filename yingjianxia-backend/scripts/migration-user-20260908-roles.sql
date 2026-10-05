-- ============================================================
-- 硬件侠平台 - 用户角色表 + 审核员账号初始化
-- 迁移时间: 2026-09-08
-- 关联 BUG: bug-20260908102434.md (BUG-001/006)
-- 说明: 创建 user_roles 表，初始化审核员账号(ID=20001)
-- 执行方式: mysql -u root -p123456 db_user < migration-user-20260908-roles.sql
-- ============================================================

USE `db_user`;

-- 1. 创建 user_roles 表
CREATE TABLE IF NOT EXISTS `user_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(32) NOT NULL COMMENT '角色: BUYER/SELLER/AUDITOR/ADMIN',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色表';

-- 2. 为所有现有用户添加 BUYER 角色
INSERT INTO `user_roles` (`user_id`, `role`)
SELECT `id`, 'BUYER' FROM `users` WHERE `id` NOT IN (
  SELECT `user_id` FROM `user_roles` WHERE `role` = 'BUYER'
)
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- 3. 为已开通店铺的用户添加 SELLER 角色
INSERT INTO `user_roles` (`user_id`, `role`)
SELECT DISTINCT `seller_id`, 'SELLER' FROM `shops` WHERE `seller_id` IS NOT NULL
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- 4. 创建审核员账号（ID=20001）
-- 注意：users 表有 uk_phone 唯一索引，需要先删除可能存在的旧记录
DELETE FROM `users` WHERE `id` = 20001;
INSERT INTO `users` (`id`, `phone`, `password_hash`, `nickname`, `gender`, `credit_score`, `transaction_count`, `monthly_income`, `is_seller_verified`, `status`, `created_at`, `updated_at`, `deleted`, `version`)
VALUES (
  20001,
  '13800002001',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7pVmZcJQqVbqVQqKb1qVbq',
  '平台审核员01',
  'secret',
  5.0,
  0,
  0,
  0,
  1,
  NOW(),
  NOW(),
  0,
  0
);

-- 5. 为审核员添加 AUDITOR 角色
INSERT INTO `user_roles` (`user_id`, `role`)
VALUES (20001, 'AUDITOR')
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- 6. 同时保留 BUYER 角色（审核员也是买家）
INSERT INTO `user_roles` (`user_id`, `role`)
VALUES (20001, 'BUYER')
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- 验证
SELECT u.id, u.phone, u.nickname, u.status, GROUP_CONCAT(r.role) AS roles
FROM `users` u
LEFT JOIN `user_roles` r ON u.id = r.user_id
WHERE u.id = 20001
GROUP BY u.id;

-- 统计
SELECT COUNT(*) AS total_roles FROM `user_roles`;
