-- Sprint 09：内容定时发布。状态仍为 DRAFT，计划时间单独存放。

ALTER TABLE content
    ADD COLUMN scheduled_publish_time DATETIME(3) NULL;

CREATE INDEX idx_content_scheduled_publish_time ON content (scheduled_publish_time);
