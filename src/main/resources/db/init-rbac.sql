-- RBAC 最小初始化（兼容 users.role）
-- 执行前请确认已存在表：roles/permissions/user_role/role_permission

INSERT INTO roles (role_code, role_name)
SELECT 'admin', '管理员' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'admin');

INSERT INTO roles (role_code, role_name)
SELECT 'nurse_leader', '护士长' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'nurse_leader');

INSERT INTO roles (role_code, role_name)
SELECT 'nurse', '护士' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'nurse');

INSERT INTO roles (role_code, role_name)
SELECT 'caregiver', '护工' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'caregiver');

INSERT INTO roles (role_code, role_name)
SELECT 'doctor', '医生' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'doctor');

INSERT INTO permissions (perm_code, perm_name, perm_type)
SELECT 'alarm:read', '报警查看', 'api' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE perm_code = 'alarm:read');

INSERT INTO permissions (perm_code, perm_name, perm_type)
SELECT 'alarm:handle', '报警处理', 'api' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE perm_code = 'alarm:handle');

INSERT INTO permissions (perm_code, perm_name, perm_type)
SELECT 'rectification:read', '整改查看', 'api' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE perm_code = 'rectification:read');

INSERT INTO permissions (perm_code, perm_name, perm_type)
SELECT 'rectification:handle', '整改处理', 'api' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE perm_code = 'rectification:handle');

INSERT INTO permissions (perm_code, perm_name, perm_type)
SELECT 'qc:manage', '质控管理', 'api' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE perm_code = 'qc:manage');

INSERT INTO permissions (perm_code, perm_name, perm_type)
SELECT 'audit:read', '审计查看', 'api' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE perm_code = 'audit:read');

INSERT INTO permissions (perm_code, perm_name, perm_type)
SELECT 'medication:manage', '药品库管理', 'api' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE perm_code = 'medication:manage');

INSERT INTO role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id
FROM roles r
JOIN permissions p ON p.perm_code IN ('alarm:read', 'alarm:handle', 'rectification:read', 'rectification:handle', 'qc:manage', 'audit:read', 'medication:manage')
WHERE r.role_code IN ('admin', 'nurse_leader')
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.role_id AND rp.perm_id = p.perm_id
  );

INSERT INTO role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id
FROM roles r
JOIN permissions p ON p.perm_code IN ('alarm:read', 'alarm:handle', 'rectification:read', 'rectification:handle', 'audit:read')
WHERE r.role_code IN ('nurse', 'caregiver')
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.role_id AND rp.perm_id = p.perm_id
  );

INSERT INTO role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id
FROM roles r
JOIN permissions p ON p.perm_code IN ('alarm:read', 'rectification:read', 'audit:read')
WHERE r.role_code = 'doctor'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.role_id AND rp.perm_id = p.perm_id
  );

-- 可选：把 users.role 映射到 user_role（仅新增缺失映射）
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_code = u.role
WHERE u.role IN ('admin', 'nurse_leader', 'nurse', 'caregiver', 'doctor')
  AND NOT EXISTS (
    SELECT 1 FROM user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
  );
