-- ============================================================
-- 1.0.9 消息发图 — 回滚
-- ============================================================
-- 用法: mysql -u root -p your_db < rollback_1.0.9.sql
-- 效果: 删掉 image_url 列,删除备份表
-- 警告: 跑完这条后,Android 1.0.9 客户端发的带图消息会丢失 image_url 字段
--       (实际留言内容/对方收到不受影响,只是图片不显示)
-- ============================================================

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME   = 'messages'
    AND COLUMN_NAME  = 'image_url'
);
SET @sql := IF(@col_exists = 1,
  'ALTER TABLE `messages` DROP COLUMN `image_url`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS `messages_backup_20261016`;
