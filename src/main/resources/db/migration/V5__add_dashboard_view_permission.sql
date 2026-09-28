-- Dashboard 运营看板查看权限。管理员角色同步获得该权限。

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 110, 'DASHBOARD_VIEW', '查看运营看板', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'DASHBOARD_VIEW' AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 310, 1, 110, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission
    WHERE role_id = 1 AND permission_id = 110 AND deleted = 0
);
