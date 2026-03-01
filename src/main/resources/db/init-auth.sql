-- 初始化一个 admin 用户，明文密码：Admin@123
INSERT INTO users (
  username,
  password_hash,
  role,
  status,
  real_name,
  phone,
  email,
  avatar_url,
  last_login_at,
  created_at,
  updated_at
) VALUES (
  'admin',
  '$2y$10$ytIGraTiUU/WdOIwR//6j.ifSvwe7FOdQ6Qw9wsBtmGgVA1DCzdXG',
  'admin',
  'active',
  '系统管理员',
  '13800000000',
  'admin@example.com',
  NULL,
  NULL,
  NOW(),
  NOW()
);
