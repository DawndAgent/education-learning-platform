-- Sprint 01：基础系统表。不创建分类、内容、文章、视频等业务表。
-- 每张表包含审计字段：created_at、updated_at、created_by、updated_by、deleted。

CREATE TABLE sys_user (
    id            BIGINT       NOT NULL,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64)  NULL,
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME(3)  NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    created_by    BIGINT       NULL,
    updated_by    BIGINT       NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL,
    role_code   VARCHAR(64)  NOT NULL,
    role_name   VARCHAR(64)  NOT NULL,
    created_at  DATETIME(3)  NOT NULL,
    updated_at  DATETIME(3)  NOT NULL,
    created_by  BIGINT       NULL,
    updated_by  BIGINT       NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_code UNIQUE (role_code)
);

CREATE TABLE sys_permission (
    id               BIGINT       NOT NULL,
    permission_code  VARCHAR(128) NOT NULL,
    permission_name  VARCHAR(64)  NOT NULL,
    created_at       DATETIME(3)  NOT NULL,
    updated_at       DATETIME(3)  NOT NULL,
    created_by       BIGINT       NULL,
    updated_by       BIGINT       NULL,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_permission_code UNIQUE (permission_code)
);

CREATE TABLE sys_user_role (
    id         BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    role_id    BIGINT      NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    created_by BIGINT      NULL,
    updated_by BIGINT      NULL,
    deleted    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_sys_user_role_role_id ON sys_user_role (role_id);

CREATE TABLE sys_role_permission (
    id            BIGINT      NOT NULL,
    role_id       BIGINT      NOT NULL,
    permission_id BIGINT      NOT NULL,
    created_at    DATETIME(3) NOT NULL,
    updated_at    DATETIME(3) NOT NULL,
    created_by    BIGINT      NULL,
    updated_by    BIGINT      NULL,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_sys_role_permission_permission_id ON sys_role_permission (permission_id);
