-- Sprint 10：首页运营配置。内容模型不变，只增加首页展示配置。

CREATE TABLE home_banner (
    id           BIGINT         NOT NULL,
    title        VARCHAR(200)   NOT NULL,
    subtitle     VARCHAR(500)   NULL,
    image_url    VARCHAR(500)   NOT NULL,
    link_type    VARCHAR(30)    NOT NULL,
    link_id      BIGINT         NULL,
    link_url     VARCHAR(1000)  NULL,
    sort         INT            NOT NULL DEFAULT 0,
    status       VARCHAR(30)    NOT NULL,
    start_time   DATETIME(3)    NULL,
    end_time     DATETIME(3)    NULL,
    created_at   DATETIME(3)    NOT NULL,
    updated_at   DATETIME(3)    NOT NULL,
    created_by   BIGINT         NULL,
    updated_by   BIGINT         NULL,
    deleted      TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_banner_status ON home_banner (status);
CREATE INDEX idx_banner_time ON home_banner (start_time, end_time);
CREATE INDEX idx_banner_sort ON home_banner (sort);
CREATE INDEX idx_banner_deleted ON home_banner (deleted);

CREATE TABLE home_recommendation (
    id              BIGINT       NOT NULL,
    title           VARCHAR(200) NULL,
    recommend_type  VARCHAR(30)  NOT NULL,
    target_id       BIGINT       NOT NULL,
    sort            INT          NOT NULL DEFAULT 0,
    status          VARCHAR(30)  NOT NULL,
    created_at      DATETIME(3)  NOT NULL,
    updated_at      DATETIME(3)  NOT NULL,
    created_by      BIGINT       NULL,
    updated_by      BIGINT       NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_recommend_target ON home_recommendation (recommend_type, target_id, deleted);
CREATE INDEX idx_recommend_type ON home_recommendation (recommend_type);
CREATE INDEX idx_recommend_status ON home_recommendation (status);
CREATE INDEX idx_recommend_sort ON home_recommendation (sort);
CREATE INDEX idx_recommend_deleted ON home_recommendation (deleted);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 130, 'HOME_OPERATION_VIEW', '查看首页运营', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'HOME_OPERATION_VIEW' AND deleted = 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 131, 'HOME_OPERATION_MANAGE', '管理首页运营', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'HOME_OPERATION_MANAGE' AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 330, 1, 130, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 130 AND deleted = 0);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
)
SELECT 331, 1, 131, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), NULL, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 131 AND deleted = 0);
