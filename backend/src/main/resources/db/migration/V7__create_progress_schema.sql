-- V7__create_progress_schema.sql
-- Progress schema: body_progress

CREATE TABLE body_progress (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    member_id       BIGINT          NOT NULL,
    record_date     DATE            NOT NULL,
    weight_kg       DECIMAL(6,2)    NOT NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_body_progress             PRIMARY KEY (id),
    CONSTRAINT fk_body_progress_member      FOREIGN KEY (member_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_body_progress_member_date UNIQUE (member_id, record_date),
    CONSTRAINT chk_body_progress_weight     CHECK (weight_kg > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
