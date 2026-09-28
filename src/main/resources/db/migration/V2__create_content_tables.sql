-- Sprint 02：分类、内容、文章、视频。重复执行不会插入第二份初始化数据。

CREATE TABLE category (
    id          BIGINT       NOT NULL,
    parent_id   BIGINT       NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    icon_url    VARCHAR(255) NULL,
    description VARCHAR(255) NULL,
    sort        INT          NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    created_at  DATETIME(3)  NOT NULL,
    updated_at  DATETIME(3)  NOT NULL,
    created_by  BIGINT       NULL,
    updated_by  BIGINT       NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_code UNIQUE (code)
);

CREATE INDEX idx_category_parent_sort ON category (parent_id, sort);

CREATE TABLE content (
    id             BIGINT        NOT NULL,
    title          VARCHAR(128)  NOT NULL,
    content_type   VARCHAR(32)   NOT NULL,
    category_id    BIGINT        NOT NULL,
    cover_url      VARCHAR(255)  NULL,
    summary        VARCHAR(512)  NULL,
    status         VARCHAR(32)   NOT NULL,
    sort           INT           NOT NULL,
    view_count     BIGINT        NOT NULL DEFAULT 0,
    favorite_count BIGINT        NOT NULL DEFAULT 0,
    publish_time   DATETIME(3)   NULL,
    created_at     DATETIME(3)   NOT NULL,
    updated_at     DATETIME(3)   NOT NULL,
    created_by     BIGINT        NULL,
    updated_by     BIGINT        NULL,
    deleted        TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_content_category_status ON content (category_id, status, sort);
CREATE INDEX idx_content_type_status ON content (content_type, status);

CREATE TABLE article (
    id         BIGINT       NOT NULL,
    content_id BIGINT       NOT NULL,
    body       TEXT         NOT NULL,
    author     VARCHAR(64)  NULL,
    source     VARCHAR(128) NULL,
    created_at DATETIME(3)  NOT NULL,
    updated_at DATETIME(3)  NOT NULL,
    created_by BIGINT       NULL,
    updated_by BIGINT       NULL,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_article_content_id UNIQUE (content_id)
);

CREATE TABLE video (
    id          BIGINT       NOT NULL,
    content_id  BIGINT       NOT NULL,
    title       VARCHAR(128) NOT NULL,
    cover_url   VARCHAR(255) NULL,
    source_type VARCHAR(32)  NOT NULL,
    video_url   VARCHAR(512) NOT NULL,
    qr_code_url VARCHAR(512) NULL,
    duration    INT          NULL,
    status      VARCHAR(32)  NOT NULL,
    created_at  DATETIME(3)  NOT NULL,
    updated_at  DATETIME(3)  NOT NULL,
    created_by  BIGINT       NULL,
    updated_by  BIGINT       NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_video_content_id UNIQUE (content_id)
);

INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 1, 0, '剑桥英语', 'CAMBRIDGE', NULL, NULL, 1, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAMBRIDGE' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2, 0, '数学思维', 'MATH', NULL, NULL, 2, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'MATH' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 3, 0, '初中自主学习', 'JUNIOR', NULL, NULL, 3, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'JUNIOR' AND deleted = 0);

INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 11, 1, 'KET/PET备考资料', 'CAMBRIDGE_KET', NULL, NULL, 1, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAMBRIDGE_KET' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 12, 1, '剑桥原版阅读', 'CAMBRIDGE_READING', NULL, NULL, 2, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAMBRIDGE_READING' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 13, 1, '听力视频课', 'CAMBRIDGE_LISTENING', NULL, NULL, 3, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAMBRIDGE_LISTENING' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 14, 1, '往期文章合集', 'CAMBRIDGE_ARCHIVE', NULL, NULL, 4, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'CAMBRIDGE_ARCHIVE' AND deleted = 0);

INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 21, 2, '思维培优习题', 'MATH_EXERCISE', NULL, NULL, 1, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'MATH_EXERCISE' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 22, 2, '竞赛专题讲解', 'MATH_CONTEST', NULL, NULL, 2, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'MATH_CONTEST' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 23, 2, '视频解析', 'MATH_VIDEO', NULL, NULL, 3, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'MATH_VIDEO' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 24, 2, '每周一题', 'MATH_WEEKLY', NULL, NULL, 4, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'MATH_WEEKLY' AND deleted = 0);

INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 31, 3, '七年级｜资料&视频', 'JUNIOR_GRADE_7', NULL, NULL, 1, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'JUNIOR_GRADE_7' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 32, 3, '八年级｜资料&视频', 'JUNIOR_GRADE_8', NULL, NULL, 2, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'JUNIOR_GRADE_8' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 33, 3, '九年级｜资料&视频', 'JUNIOR_GRADE_9', NULL, NULL, 3, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'JUNIOR_GRADE_9' AND deleted = 0);
INSERT INTO category (
    id, parent_id, name, code, icon_url, description, sort, status,
    created_at, updated_at, created_by, updated_by, deleted
)
SELECT 34, 3, '中考/自招专栏', 'JUNIOR_EXAM', NULL, NULL, 4, 'ENABLED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'JUNIOR_EXAM' AND deleted = 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 101, 'CONTENT_VIEW', '查看内容', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'CONTENT_VIEW' AND deleted = 0);
INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 102, 'CONTENT_CREATE', '创建内容', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'CONTENT_CREATE' AND deleted = 0);
INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 103, 'CONTENT_UPDATE', '修改内容', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'CONTENT_UPDATE' AND deleted = 0);
INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 104, 'CONTENT_DELETE', '删除内容', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'CONTENT_DELETE' AND deleted = 0);
INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 105, 'CONTENT_PUBLISH', '发布内容', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'CONTENT_PUBLISH' AND deleted = 0);
INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 106, 'CONTENT_OFFLINE', '下架内容', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'CONTENT_OFFLINE' AND deleted = 0);
INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 107, 'CATEGORY_MANAGE', '管理分类', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'CATEGORY_MANAGE' AND deleted = 0);
INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 108, 'VIDEO_MANAGE', '管理视频', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'VIDEO_MANAGE' AND deleted = 0);
