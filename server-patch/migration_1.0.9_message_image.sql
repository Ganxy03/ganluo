-- ============================================================
-- 1.0.9 消息发图 — 数据库迁移(幂等,可重复执行)
-- ============================================================
-- 作用:messages 表加 image_url 可空字段
-- 不破坏性:
--   * 老数据(纯文本消息)保持不变,新列默认为 NULL
--   * 现有小程序端发的纯文本消息继续工作(后端 INSERT 时该列存 NULL)
--   * 现有小程序端读 messages 时,JSON 响应多一个 image_url 字段
--     标准 JSON 解析忽略未知字段,小程序不会崩(它代码里没读这字段)
-- 幂等性:
--   * IF NOT EXISTS 保护:列已存在则跳过(可重复跑)
-- 回滚:
--   * 跑 rollback.sql
-- ============================================================

-- 1. 备份 messages 表(以防万一,只备份结构 + 最近 1000 条做参考)
CREATE TABLE IF NOT EXISTS `messages_backup_20261016` LIKE `messages`;
INSERT IGNORE INTO `messages_backup_20261016`
  SELECT * FROM `messages`
  ORDER BY id DESC LIMIT 1000;

-- 2. 加 image_url 列(幂等:列已存在则跳过)
-- MySQL 8.0 不支持直接 IF NOT EXISTS in ADD COLUMN,需要用 INFORMATION_SCHEMA 判定
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME   = 'messages'
    AND COLUMN_NAME  = 'image_url'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `messages` ADD COLUMN `image_url` VARCHAR(255) DEFAULT NULL COMMENT ''留言配图(可选)'' AFTER `content`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 验证
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME   = 'messages'
    AND COLUMN_NAME  = 'image_url';

-- 期望输出:image_url / varchar / YES / NULL
