-- Sprint 07：专题聚合。V5 之后下一个版本。

CREATE TABLE topic (
    id           BIGINT        NOT NULL,
    name         VARCHAR(200)  NOT NULL,
    code         VARCHAR(100)  NOT NULL,
    cover_url    VARCHAR(500)  NULL,
    summary      VARCHAR(500)  NULL,
    category_id  BIGINT        NOT NULL,
    status       VARCHAR(30)   NOT NULL,
    sort         INT           NOT NULL DEFAULT 0,
    publish_time DATETIME(3)   NULL,
    created_at   DATETIME(3)   NOT NULL,
    updated_at   DATETIME(3)   NOT NULL,
    created_by   BIGINT        NULL,
    updated_by   BIGINT        NULL,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_topic_code UNIQUE (code)
);

CREATE INDEX idx_topic_category_id ON topic (category_id);
CREATE INDEX idx_topic_status ON topic (status);
CREATE INDEX idx_topic_sort ON topic (sort);
CREATE INDEX idx_topic_deleted ON topic (deleted);

CREATE TABLE topic_content (
    id         BIGINT      NOT NULL,
    topic_id   BIGINT      NOT NULL,
    content_id BIGINT      NOT NULL,
    sort       INT         NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    created_by BIGINT      NULL,
    updated_by BIGINT      NULL,
    deleted    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_topic_content ON topic_content (topic_id, content_id, deleted);
CREATE INDEX idx_topic_content_topic ON topic_content (topic_id);
CREATE INDEX idx_topic_content_content ON topic_content (content_id);
CREATE INDEX idx_topic_content_sort ON topic_content (topic_id, sort);
CREATE INDEX idx_topic_content_deleted ON topic_content (deleted);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 120, 'TOPIC_VIEW', '查看专题', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'TOPIC_VIEW' AND deleted = 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 121, 'TOPIC_CREATE', '创建专题', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'TOPIC_CREATE' AND deleted = 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 122, 'TOPIC_UPDATE', '编辑专题', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'TOPIC_UPDATE' AND deleted = 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 123, 'TOPIC_DELETE', '删除专题', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'TOPIC_DELETE' AND deleted = 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 124, 'TOPIC_PUBLISH', '发布专题', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'TOPIC_PUBLISH' AND deleted = 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 125, 'TOPIC_OFFLINE', '下线专题', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'TOPIC_OFFLINE' AND deleted = 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 126, 'TOPIC_CONTENT_MANAGE', '管理专题内容', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'TOPIC_CONTENT_MANAGE' AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 320, 1, 120, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 120 AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 321, 1, 121, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 121 AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 322, 1, 122, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 122 AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 323, 1, 123, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 123 AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 324, 1, 124, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 124 AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 325, 1, 125, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 125 AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 326, 1, 126, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 126 AND deleted = 0);
