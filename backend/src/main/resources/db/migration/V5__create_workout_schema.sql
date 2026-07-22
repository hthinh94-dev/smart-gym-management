-- V5__create_workout_schema.sql
-- Workout schema: workout_plans, workout_days, workout_plan_exercises, workout_sessions, workout_logs

CREATE TABLE workout_plans (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    member_id               BIGINT      NOT NULL,
    plan_name               VARCHAR(150) NOT NULL,
    split_model             VARCHAR(100) NOT NULL,
    goal                    VARCHAR(20) NOT NULL,
    explanation             TEXT        NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    recommendation_source   VARCHAR(30) NOT NULL DEFAULT 'AI_GENERATED',
    activated_at            TIMESTAMP(6) NULL,
    version                 BIGINT      NOT NULL DEFAULT 0,
    active_member_key       BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'ACTIVE' THEN member_id ELSE NULL END
    ) STORED,
    created_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_workout_plans                     PRIMARY KEY (id),
    CONSTRAINT fk_workout_plans_member              FOREIGN KEY (member_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_workout_plans_one_active          UNIQUE (active_member_key),
    CONSTRAINT chk_workout_plans_goal               CHECK (goal IN ('BULK', 'CUT', 'MAINTAIN')),
    CONSTRAINT chk_workout_plans_status             CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_workout_plans_rec_source         CHECK (recommendation_source IN ('MANUAL', 'AI_GENERATED', 'FALLBACK_TEMPLATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_days (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    workout_plan_id BIGINT      NOT NULL,
    day_number      TINYINT     NOT NULL,
    day_name        VARCHAR(100) NOT NULL,
    focus           VARCHAR(150) NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_workout_days              PRIMARY KEY (id),
    CONSTRAINT fk_workout_days_plan         FOREIGN KEY (workout_plan_id) REFERENCES workout_plans (id) ON DELETE CASCADE,
    CONSTRAINT uk_workout_days_plan_number  UNIQUE (workout_plan_id, day_number),
    CONSTRAINT chk_workout_days_day_number  CHECK (day_number BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_plan_exercises (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    workout_day_id  BIGINT          NOT NULL,
    exercise_id     BIGINT          NOT NULL,
    exercise_order  SMALLINT        NOT NULL,
    planned_sets    TINYINT         NOT NULL,
    planned_reps    SMALLINT        NOT NULL,
    planned_rpe     DECIMAL(3,1)    NOT NULL,
    rest_seconds    SMALLINT        NOT NULL,
    notes           TEXT            NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_workout_plan_exercises                PRIMARY KEY (id),
    CONSTRAINT fk_workout_plan_exercises_day            FOREIGN KEY (workout_day_id) REFERENCES workout_days (id)  ON DELETE CASCADE,
    CONSTRAINT fk_workout_plan_exercises_exercise       FOREIGN KEY (exercise_id)    REFERENCES exercises (id)     ON DELETE RESTRICT,
    CONSTRAINT uk_workout_plan_exercises_day_order      UNIQUE (workout_day_id, exercise_order),
    CONSTRAINT uk_workout_plan_exercises_day_exercise   UNIQUE (workout_day_id, exercise_id),
    CONSTRAINT chk_workout_plan_exercises_sets          CHECK (planned_sets BETWEEN 1 AND 5),
    CONSTRAINT chk_workout_plan_exercises_reps          CHECK (planned_reps BETWEEN 1 AND 30),
    CONSTRAINT chk_workout_plan_exercises_rpe           CHECK (planned_rpe BETWEEN 6.0 AND 9.0),
    CONSTRAINT chk_workout_plan_exercises_rest          CHECK (rest_seconds BETWEEN 30 AND 300),
    CONSTRAINT chk_workout_plan_exercises_order         CHECK (exercise_order > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_sessions (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    workout_day_id  BIGINT      NOT NULL,
    session_date    DATE        NOT NULL,
    notes           TEXT        NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_workout_sessions                  PRIMARY KEY (id),
    CONSTRAINT fk_workout_sessions_member           FOREIGN KEY (member_id)      REFERENCES users (id)       ON DELETE RESTRICT,
    CONSTRAINT fk_workout_sessions_workout_day      FOREIGN KEY (workout_day_id) REFERENCES workout_days (id) ON DELETE RESTRICT,
    CONSTRAINT uk_workout_sessions_member_date      UNIQUE (member_id, session_date),
    CONSTRAINT uk_workout_sessions_identity         UNIQUE (id, member_id, session_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_logs (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    workout_session_id          BIGINT          NOT NULL,
    member_id                   BIGINT          NOT NULL,
    log_date                    DATE            NOT NULL,
    workout_plan_exercise_id    BIGINT          NOT NULL,
    exercise_id                 BIGINT          NOT NULL,
    actual_sets                 TINYINT         NOT NULL,
    actual_reps                 SMALLINT        NOT NULL,
    actual_rpe                  DECIMAL(3,1)    NOT NULL,
    weight_used_kg              DECIMAL(7,2)    NOT NULL,
    notes                       TEXT            NULL,
    created_at                  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_workout_logs                              PRIMARY KEY (id),
    CONSTRAINT fk_workout_logs_session_identity             FOREIGN KEY (workout_session_id, member_id, log_date) REFERENCES workout_sessions (id, member_id, session_date) ON DELETE CASCADE,
    CONSTRAINT fk_workout_logs_member                       FOREIGN KEY (member_id)                REFERENCES users (id)                  ON DELETE RESTRICT,
    CONSTRAINT fk_workout_logs_plan_exercise                FOREIGN KEY (workout_plan_exercise_id) REFERENCES workout_plan_exercises (id) ON DELETE RESTRICT,
    CONSTRAINT fk_workout_logs_exercise                     FOREIGN KEY (exercise_id)              REFERENCES exercises (id)              ON DELETE RESTRICT,
    CONSTRAINT uk_workout_logs_member_date_exercise         UNIQUE (member_id, log_date, exercise_id),
    CONSTRAINT chk_workout_logs_actual_sets                 CHECK (actual_sets BETWEEN 1 AND 10),
    CONSTRAINT chk_workout_logs_actual_reps                 CHECK (actual_reps BETWEEN 1 AND 100),
    CONSTRAINT chk_workout_logs_actual_rpe                  CHECK (actual_rpe BETWEEN 1.0 AND 10.0),
    CONSTRAINT chk_workout_logs_weight                      CHECK (weight_used_kg >= 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_workout_plans_member_status
    ON workout_plans (member_id, status);

CREATE INDEX idx_workout_logs_plan_exercise
    ON workout_logs (workout_plan_exercise_id);

CREATE INDEX idx_workout_logs_exercise_date
    ON workout_logs (exercise_id, log_date);
