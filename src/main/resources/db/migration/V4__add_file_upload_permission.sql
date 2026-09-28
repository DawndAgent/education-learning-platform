-- 文件上传权限。管理员角色同步获得该权限。

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 109, 'FILE_UPLOAD', '上传文件', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FILE_UPLOAD' AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 309, 1, 109, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission
    WHERE role_id = 1 AND permission_id = 109 AND deleted = 0
);
