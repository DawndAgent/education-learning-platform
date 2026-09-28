-- 本地开发默认管理员。password_hash 是 BCrypt 密文，明文只写在 README。
-- 生产环境部署后必须立刻修改这个账号的密码。

INSERT INTO sys_role (
    id, role_code, role_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 1, 'ADMIN', '管理员', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'ADMIN' AND deleted = 0);

INSERT INTO sys_user (
    id, username, password_hash, nickname, status, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 1, 'admin', '$2a$10$lB/26dYn6x1QywlQDX3B2e8T/M0OBCQ8OihSNCcer5nBblqiHn8Vy', '管理员', 1,
       CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin' AND deleted = 0);

INSERT INTO sys_user_role (
    id, user_id, role_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 1, 1, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = 1 AND deleted = 0
);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 200 + p.id, 1, p.id, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
FROM sys_permission p
WHERE p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = 1 AND rp.permission_id = p.id AND rp.deleted = 0
  );
