-- V4__create_exercise_schema.sql
-- Exercise schema: exercises and 4 metadata collection tables

CREATE TABLE exercises (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    name                VARCHAR(150) NOT NULL,
    normalized_name     VARCHAR(150) NOT NULL,
    primary_muscle_group VARCHAR(50) NOT NULL,
    movement_pattern    VARCHAR(50) NOT NULL,
    difficulty_level    VARCHAR(20) NOT NULL,
    instruction_text    TEXT        NOT NULL,
    is_active           TINYINT(1)  NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_exercises                     PRIMARY KEY (id),
    CONSTRAINT uk_exercises_normalized_name     UNIQUE (normalized_name),
    CONSTRAINT chk_exercises_primary_muscle     CHECK (primary_muscle_group IN ('CHEST', 'BACK', 'SHOULDERS', 'ARMS', 'LEGS', 'GLUTES', 'CORE', 'CARDIO', 'FULL_BODY')),
    CONSTRAINT chk_exercises_movement_pattern   CHECK (movement_pattern IN ('PUSH', 'PULL', 'SQUAT', 'HINGE', 'LUNGE', 'CARRY', 'ROTATION')),
    CONSTRAINT chk_exercises_difficulty         CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_exercises_active             CHECK (is_active IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exercise_secondary_muscles (
    exercise_id     BIGINT      NOT NULL,
    muscle_group    VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_exercise_secondary_muscles            PRIMARY KEY (exercise_id, muscle_group),
    CONSTRAINT fk_exercise_secondary_muscles_exercise   FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT chk_exercise_secondary_muscles_value     CHECK (muscle_group IN ('CHEST', 'BACK', 'SHOULDERS', 'ARMS', 'LEGS', 'GLUTES', 'CORE', 'CARDIO', 'FULL_BODY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exercise_equipment (
    exercise_id BIGINT      NOT NULL,
    equipment   VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_exercise_equipment            PRIMARY KEY (exercise_id, equipment),
    CONSTRAINT fk_exercise_equipment_exercise   FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT chk_exercise_equipment_value     CHECK (equipment IN ('BARBELL', 'DUMBBELL', 'MACHINE', 'CABLE', 'BENCH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exercise_target_body_regions (
    exercise_id BIGINT      NOT NULL,
    body_region VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_exercise_target_body_regions          PRIMARY KEY (exercise_id, body_region),
    CONSTRAINT fk_exercise_target_body_regions_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT chk_exercise_target_body_regions_value   CHECK (body_region IN ('UPPER_BODY', 'LOWER_BODY', 'CORE', 'FULL_BODY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exercise_contraindication_tags (
    exercise_id         BIGINT      NOT NULL,
    contraindication_tag VARCHAR(80) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_exercise_contraindication_tags            PRIMARY KEY (exercise_id, contraindication_tag),
    CONSTRAINT fk_exercise_contraindication_tags_exercise   FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT chk_exercise_contraindication_tags_value     CHECK (contraindication_tag IN (
        'KNEE_FLEXION_LIMITED',
        'OVERHEAD_MOVEMENT_LIMITED',
        'LOWER_BACK_LOAD_LIMITED',
        'WRIST_FLEXION_LIMITED',
        'NECK_LOAD_LIMITED'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_exercises_active_difficulty
    ON exercises (is_active, difficulty_level);

CREATE INDEX idx_exercises_primary_muscle_active
    ON exercises (primary_muscle_group, is_active);

CREATE INDEX idx_exercise_equipment_equipment
    ON exercise_equipment (equipment, exercise_id);

CREATE INDEX idx_exercise_contraindications_tag
    ON exercise_contraindication_tags (contraindication_tag, exercise_id);
