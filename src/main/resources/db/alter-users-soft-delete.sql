-- users 表软删除字段
ALTER TABLE users
  ADD COLUMN deleted_at DATETIME NULL AFTER updated_at;

-- 常用筛选索引（可选但建议）
CREATE INDEX idx_users_deleted_at ON users (deleted_at);
