-- =========================================================
--  吉壹 · 情侣私密日常记录工具 —— 数据库结构
--  MySQL 5.7+ / 8.0
--  导入： mysql -u root -p < sql/schema.sql
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `ganluo`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `ganluo`;

-- ---------------------------------------------------------
-- 用户
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `openid`      VARCHAR(64)     NOT NULL                COMMENT '微信小程序 openid',
  `unionid`     VARCHAR(64)     DEFAULT NULL            COMMENT '微信开放平台 unionid',
  `nickname`    VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '昵称',
  `avatar`      VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '头像 URL',
  `invite_code` VARCHAR(16)     NOT NULL                COMMENT '用户ID，对方凭此搜索并绑定',
  `signature`   VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '个性签名',
  `partner_id`  BIGINT UNSIGNED DEFAULT NULL            COMMENT '伴侣的 user_id，NULL 表示未绑定',
  `bind_at`     DATETIME        DEFAULT NULL            COMMENT '绑定成功时间（可当纪念日）',
  `status`      TINYINT         NOT NULL DEFAULT 1      COMMENT '1=正常 0=禁用',
  `created_at`  DATETIME        NOT NULL,
  `updated_at`  DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_partner` (`partner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ---------------------------------------------------------
-- 绑定请求
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bind_requests` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `from_user_id` BIGINT UNSIGNED NOT NULL              COMMENT '发起方',
  `to_user_id`   BIGINT UNSIGNED NOT NULL              COMMENT '接收方',
  `status`       TINYINT         NOT NULL DEFAULT 0    COMMENT '0=待处理 1=已同意 2=已拒绝 3=已撤销',
  `created_at`   DATETIME        NOT NULL,
  `updated_at`   DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_to_status` (`to_user_id`, `status`),
  KEY `idx_from_status` (`from_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定请求';

-- ---------------------------------------------------------
-- 心情日记
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `moods` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `mood_type`  VARCHAR(32)     NOT NULL              COMMENT 'happy/sweet/calm/excited/tired/sad/angry/anxious',
  `content`    VARCHAR(500)    NOT NULL DEFAULT ''   COMMENT '心情文字',
  `mood_date`  DATE            NOT NULL              COMMENT '归属日期，用于按天展示',
  `created_at` DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `mood_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心情日记';

-- ---------------------------------------------------------
-- 三餐报备
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `meals` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `meal_type`  VARCHAR(16)     NOT NULL              COMMENT 'breakfast/lunch/dinner/supper',
  `content`    VARCHAR(500)    NOT NULL DEFAULT ''   COMMENT '文字描述',
  `images`     JSON            DEFAULT NULL          COMMENT '图片 URL 数组，最多 9 张',
  `meal_date`  DATE            NOT NULL              COMMENT '归属日期',
  `created_at` DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_meal_date` (`user_id`, `meal_type`, `meal_date`),
  KEY `idx_user_date` (`user_id`, `meal_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三餐报备';

-- ---------------------------------------------------------
-- 留言提醒
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `messages` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `from_user_id` BIGINT UNSIGNED NOT NULL,
  `to_user_id`   BIGINT UNSIGNED NOT NULL,
  `reply_to_id`  BIGINT UNSIGNED DEFAULT NULL        COMMENT '回复的是哪条留言，NULL=新留言',
  `content`      VARCHAR(500)    NOT NULL,
  `remind_at`    DATETIME        DEFAULT NULL        COMMENT '定时提醒时间。已弃用，恒为 NULL',
  `is_read`      TINYINT         NOT NULL DEFAULT 0,
  `read_at`      DATETIME        DEFAULT NULL,
  `is_notified`  TINYINT         NOT NULL DEFAULT 0  COMMENT '已弃用',
  `created_at`   DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_to_read` (`to_user_id`, `is_read`),
  KEY `idx_remind` (`remind_at`, `is_notified`),
  KEY `idx_reply` (`reply_to_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言提醒';

-- ---------------------------------------------------------
-- 已有数据库升级用（全新部署不用执行，上面 CREATE TABLE 已含该字段）
-- ---------------------------------------------------------
-- ALTER TABLE `messages`
--   ADD COLUMN `reply_to_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '回复的是哪条留言' AFTER `to_user_id`,
--   ADD KEY `idx_reply` (`reply_to_id`),
--   ADD KEY `idx_created` (`created_at`);

SET FOREIGN_KEY_CHECKS = 1;
