-- ============================================================
-- 审核员账号初始化脚本
-- 执行方式: mysql -u root -p db_user < init-auditor-account.sql
-- ============================================================

-- 审核员账号（ID=20001，与 AuditServiceImpl.assignAuditor() 中硬编码一致）
-- 密码: Auditor@2026 (BCrypt 加密)
INSERT INTO `users` (`id`, `phone`, `password_hash`, `nickname`, `gender`, `credit_score`, `transaction_count`, `monthly_income`, `is_seller_verified`, `status`, `created_at`, `updated_at`)
VALUES (
  20001,
  '13800002001',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7pVmZcJQqVbqVQqKb1qVbq',  -- BCrypt: Auditor@2026
  '平台审核员01',
  'secret',
  5.0,
  0,
  0,
  0,
  1,  -- STATUS_NORMAL
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE `nickname` = '平台审核员01', `status` = 1, `updated_at` = NOW();

-- 审核员角色（如果 user_roles 表存在）
INSERT INTO `user_roles` (`user_id`, `role`, `created_at`, `updated_at`)
VALUES (20001, 'AUDITOR', NOW(), NOW())
ON DUPLICATE KEY UPDATE `role` = 'AUDITOR', `updated_at` = NOW();

-- 验证
SELECT id, phone, nickname, status FROM `users` WHERE id = 20001;
