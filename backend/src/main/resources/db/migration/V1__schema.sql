CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(32)  NOT NULL,
    created_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_sessions (
    token_hash CHAR(64)    NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    PRIMARY KEY (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_sessions_user ON user_sessions (user_id);

CREATE TABLE templates (
    id                   VARCHAR(64)  NOT NULL,
    title                VARCHAR(64)  NOT NULL,
    category             VARCHAR(32)  NOT NULL,
    cover_url            VARCHAR(255) NOT NULL,
    description          VARCHAR(500) NOT NULL,
    required_image_count INT          NOT NULL,
    accepted_mime_types  VARCHAR(255) NOT NULL,
    max_file_size_bytes  BIGINT       NOT NULL,
    result_asset         VARCHAR(128) NOT NULL,
    sort_order           INT          NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE upload_assets (
    id            CHAR(36)     NOT NULL,
    user_id       BIGINT       NOT NULL,
    template_id   VARCHAR(64)  NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(32)  NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    created_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_uploads_user ON upload_assets (user_id);

CREATE TABLE generation_tasks (
    id                CHAR(36)     NOT NULL,
    user_id           BIGINT       NOT NULL,
    template_id       VARCHAR(64)  NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    current_stage     VARCHAR(32)  NULL,
    error_message     VARCHAR(500) NULL,
    result_url        VARCHAR(255) NULL,
    sim_fail_stage    VARCHAR(32)  NULL,
    sim_step_delay_ms INT          NOT NULL,
    created_at        DATETIME(3)  NOT NULL,
    started_at        DATETIME(3)  NULL,
    finished_at       DATETIME(3)  NULL,
    updated_at        DATETIME(3)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_tasks_user_created ON generation_tasks (user_id, created_at);
CREATE INDEX idx_tasks_user_template_created ON generation_tasks (user_id, template_id, created_at);
CREATE INDEX idx_tasks_status ON generation_tasks (status);

CREATE TABLE task_inputs (
    task_id     CHAR(36) NOT NULL,
    input_index INT      NOT NULL,
    upload_id   CHAR(36) NOT NULL,
    PRIMARY KEY (task_id, input_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_steps (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    task_id     CHAR(36)     NOT NULL,
    stage       VARCHAR(32)  NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    message     VARCHAR(500) NULL,
    started_at  DATETIME(3)  NOT NULL,
    finished_at DATETIME(3)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_task_steps_task_stage UNIQUE (task_id, stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
