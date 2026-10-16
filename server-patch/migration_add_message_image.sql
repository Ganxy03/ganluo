-- ============================================================
-- 1.0.9 消息发图功能 — 数据库迁移
-- ============================================================
-- 作用:messages 表加 image_url 可空字段
-- 不破坏性:
--   - 新字段 NULL 默认,老数据(纯文本消息)不变
--   - 老 API 调用不传 image_url → INSERT 时该列存 NULL
--   - 老 API 响应不读 image_url → SELECT 时多一列,JSON 解析多一字段,小程序解析不报错
--   - 索引只在 image_url IS NOT NULL 时建(可选,看查询需求)
-- 兼容性:
--   - 已部署的 PHP 代码不需要立刻改(后续手动加 image_url 处理)
--   - Android 1.0.9 客户端必须配套部署改过的 messages 控制器
-- ============================================================

ALTER TABLE messages
  ADD COLUMN image_url VARCHAR(255) NULL COMMENT '留言配图(可选,小程序暂不用)' AFTER content;

-- 可选:给"找带图的留言"加速;数据量小(<1万)的话不加也无妨
-- CREATE INDEX idx_messages_image ON messages (image_url(64));
