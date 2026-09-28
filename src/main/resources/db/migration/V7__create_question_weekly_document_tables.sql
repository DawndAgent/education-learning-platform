-- Sprint 08：题目 / 每周一题 / 资料

CREATE TABLE question (
    id                  BIGINT        NOT NULL,
    content_id          BIGINT        NOT NULL,
    question_type       VARCHAR(30)   NOT NULL,
    question_text       LONGTEXT      NULL,
    question_image_url  VARCHAR(500)  NULL,
    answer_text         LONGTEXT      NULL,
    answer_image_url    VARCHAR(500)  NULL,
    analysis_text       LONGTEXT      NULL,
    analysis_image_url  VARCHAR(500)  NULL,
    difficulty          VARCHAR(30)   NULL,
    created_at          DATETIME(3)   NOT NULL,
    updated_at          DATETIME(3)   NOT NULL,
    created_by          BIGINT        NULL,
    updated_by          BIGINT        NULL,
    deleted             TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_question_content_id UNIQUE (content_id)
);

CREATE INDEX idx_question_type ON question (question_type);
CREATE INDEX idx_question_difficulty ON question (difficulty);
CREATE INDEX idx_question_deleted ON question (deleted);

CREATE TABLE weekly_question (
    id                  BIGINT        NOT NULL,
    content_id          BIGINT        NOT NULL,
    week_label          VARCHAR(50)   NULL,
    question_text       LONGTEXT      NULL,
    question_image_url  VARCHAR(500)  NULL,
    answer_text         LONGTEXT      NULL,
    answer_image_url    VARCHAR(500)  NULL,
    analysis_text       LONGTEXT      NULL,
    analysis_image_url  VARCHAR(500)  NULL,
    created_at          DATETIME(3)   NOT NULL,
    updated_at          DATETIME(3)   NOT NULL,
    created_by          BIGINT        NULL,
    updated_by          BIGINT        NULL,
    deleted             TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_weekly_content_id UNIQUE (content_id)
);

CREATE INDEX idx_weekly_week_label ON weekly_question (week_label);
CREATE INDEX idx_weekly_deleted ON weekly_question (deleted);

CREATE TABLE document (
    id            BIGINT         NOT NULL,
    content_id    BIGINT         NOT NULL,
    file_url      VARCHAR(1000)  NULL,
    file_name     VARCHAR(255)   NULL,
    file_size     BIGINT         NULL,
    file_type     VARCHAR(100)   NULL,
    download_url  VARCHAR(1000)  NULL,
    preview_url   VARCHAR(1000)  NULL,
    description   VARCHAR(1000)  NULL,
    created_at    DATETIME(3)    NOT NULL,
    updated_at    DATETIME(3)    NOT NULL,
    created_by    BIGINT         NULL,
    updated_by    BIGINT         NULL,
    deleted       TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_document_content_id UNIQUE (content_id)
);

CREATE INDEX idx_document_file_type ON document (file_type);
CREATE INDEX idx_document_deleted ON document (deleted);
