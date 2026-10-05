-- ============================================================
-- 硬件侠平台 - 数据库初始化脚本
-- 执行方式: MySQL 容器启动时自动执行 (docker-entrypoint-initdb.d)
-- 或手动: mysql -u root -p < init-databases.sql
-- ============================================================

-- 用户域
CREATE DATABASE IF NOT EXISTS `db_user`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 商品域
CREATE DATABASE IF NOT EXISTS `db_product`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 验机域
CREATE DATABASE IF NOT EXISTS `db_inspection`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 订单域
CREATE DATABASE IF NOT EXISTS `db_order`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 担保资金域
CREATE DATABASE IF NOT EXISTS `db_escrow`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 物流域
CREATE DATABASE IF NOT EXISTS `db_logistics`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 售后域
CREATE DATABASE IF NOT EXISTS `db_aftersales`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 消息与社区域
CREATE DATABASE IF NOT EXISTS `db_message`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 客服支撑+营销积分域
CREATE DATABASE IF NOT EXISTS `db_support`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 评价域
CREATE DATABASE IF NOT EXISTS `db_evaluation`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 审核域
CREATE DATABASE IF NOT EXISTS `db_audit`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 风控域
CREATE DATABASE IF NOT EXISTS `db_risk`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- 授权 root 远程访问（开发环境）
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '123456' WITH GRANT OPTION;
FLUSH PRIVILEGES;
