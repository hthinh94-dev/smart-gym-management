-- schema-draft.sql
-- Draft database schema for Smart Gym Management System
-- MySQL 8
-- Consolidated design reference aligned with Flyway V1-V10.
-- Do not execute this file against an existing Flyway-managed database.

-- Mọi TIMESTAMP được ghi/đọc theo UTC; DATE nghiệp vụ được Backend quy đổi
-- từ timezone Asia/Ho_Chi_Minh trước khi lưu.
SET time_zone = '+00:00';

-- ==============================================================================
-- 1. NHÓM AUTH: users, roles, user_roles
-- ==============================================================================

CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    account_status  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users                 PRIMARY KEY (id),
    CONSTRAINT uk_users_email           UNIQUE (email),
    CONSTRAINT chk_users_account_status CHECK (account_status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE roles (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_roles     PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name CHECK (name IN ('ROLE_ADMIN', 'ROLE_MEMBER', 'ROLE_PT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE user_roles (
    user_id     BIGINT          NOT NULL,
    role_id     BIGINT          NOT NULL,
    created_at  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_user_roles        PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role   FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- 2. NHÓM PROFILE: member_profiles và 6 bảng Collection phụ
-- ==============================================================================

CREATE TABLE member_profiles (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT          NOT NULL,
    gender                  VARCHAR(10)     NOT NULL,
    date_of_birth           DATE            NOT NULL,
    height_cm               DECIMAL(5,2)    NOT NULL,
    weight_kg               DECIMAL(6,2)    NOT NULL,
    target_weight_kg        DECIMAL(6,2)    NULL,
    mobility_limit_notes    VARCHAR(500)    NULL,
    fitness_goal            VARCHAR(20)     NOT NULL,
    fitness_level           VARCHAR(20)     NOT NULL,
    activity_level          VARCHAR(30)     NOT NULL,
    workout_days_per_week   TINYINT         NOT NULL,
    max_session_minutes     SMALLINT        NOT NULL,
    dietary_preference      VARCHAR(20)     NOT NULL,
    meals_per_day           TINYINT         NOT NULL,
    created_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_profiles               PRIMARY KEY (id),
    CONSTRAINT uk_member_profiles_user          UNIQUE (user_id),
    CONSTRAINT fk_member_profiles_user          FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_member_profiles_gender       CHECK (gender IN ('MALE', 'FEMALE')),
    CONSTRAINT chk_member_profiles_goal         CHECK (fitness_goal IN ('BULK', 'CUT', 'MAINTAIN', 'MUSCLE_GAIN', 'WEIGHT_GAIN', 'FAT_LOSS', 'WEIGHT_LOSS')),
    CONSTRAINT chk_member_profiles_level        CHECK (fitness_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_member_profiles_activity     CHECK (activity_level IN ('SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE')),
    CONSTRAINT chk_member_profiles_days         CHECK (workout_days_per_week BETWEEN 1 AND 7),
    CONSTRAINT chk_member_profiles_session      CHECK (max_session_minutes > 0),
    CONSTRAINT chk_member_profiles_dietary      CHECK (dietary_preference IN ('OMNIVORE', 'VEGETARIAN', 'VEGAN')),
    CONSTRAINT chk_member_profiles_meals        CHECK (meals_per_day BETWEEN 1 AND 6),
    CONSTRAINT chk_member_profiles_height       CHECK (height_cm > 0),
    CONSTRAINT chk_member_profiles_weight       CHECK (weight_kg > 0),
    CONSTRAINT chk_member_profiles_target_weight CHECK (target_weight_kg IS NULL OR target_weight_kg > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE member_fitness_goals (
    member_profile_id   BIGINT      NOT NULL,
    fitness_goal        VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_fitness_goals PRIMARY KEY (member_profile_id, fitness_goal),
    CONSTRAINT fk_member_fitness_goals_profile FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_member_fitness_goals_value CHECK (fitness_goal IN ('BULK', 'CUT', 'MAINTAIN', 'MUSCLE_GAIN', 'WEIGHT_GAIN', 'FAT_LOSS', 'WEIGHT_LOSS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE member_available_equipment (
    member_profile_id   BIGINT      NOT NULL,
    equipment           VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_available_equipment        PRIMARY KEY (member_profile_id, equipment),
    CONSTRAINT fk_member_available_equipment_profile FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_member_available_equipment_value CHECK (equipment IN ('BARBELL', 'DUMBBELL', 'MACHINE', 'CABLE', 'BENCH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE member_target_muscle_groups (
    member_profile_id   BIGINT      NOT NULL,
    muscle_group        VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_target_muscle_groups           PRIMARY KEY (member_profile_id, muscle_group),
    CONSTRAINT fk_member_target_muscle_groups_profile   FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_member_target_muscle_groups_value    CHECK (muscle_group IN ('CHEST', 'BACK', 'SHOULDERS', 'ARMS', 'LEGS', 'GLUTES', 'CORE', 'CARDIO', 'FULL_BODY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE member_injury_constraints (
    member_profile_id   BIGINT      NOT NULL,
    constraint_tag      VARCHAR(80) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_injury_constraints             PRIMARY KEY (member_profile_id, constraint_tag),
    CONSTRAINT fk_member_injury_constraints_profile     FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_member_injury_constraints_value      CHECK (constraint_tag IN (
        'KNEE_FLEXION_LIMITED',
        'OVERHEAD_MOVEMENT_LIMITED',
        'LOWER_BACK_LOAD_LIMITED',
        'WRIST_FLEXION_LIMITED',
        'NECK_LOAD_LIMITED'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE member_food_allergies (
    member_profile_id   BIGINT      NOT NULL,
    allergy_name        VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_food_allergies             PRIMARY KEY (member_profile_id, allergy_name),
    CONSTRAINT fk_member_food_allergies_profile     FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE member_excluded_foods (
    member_profile_id   BIGINT      NOT NULL,
    food_name           VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_excluded_foods             PRIMARY KEY (member_profile_id, food_name),
    CONSTRAINT fk_member_excluded_foods_profile     FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- 3. NHÓM MEMBERSHIP: membership_packages, member_subscriptions,
--                     subscription_renewal_requests
-- ==============================================================================

CREATE TABLE membership_packages (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    normalized_name VARCHAR(100)    NOT NULL,
    description     VARCHAR(1000)   NULL,
    duration_days   SMALLINT        NOT NULL,
    price           DECIMAL(12,2)   NOT NULL,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_membership_packages                   PRIMARY KEY (id),
    CONSTRAINT uk_membership_packages_normalized_name   UNIQUE (normalized_name),
    CONSTRAINT chk_membership_packages_duration         CHECK (duration_days BETWEEN 1 AND 3650),
    CONSTRAINT chk_membership_packages_price            CHECK (price >= 0),
    CONSTRAINT chk_membership_packages_active           CHECK (is_active IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE member_subscriptions (
    id                              BIGINT          NOT NULL AUTO_INCREMENT,
    member_id                       BIGINT          NOT NULL,
    package_id                      BIGINT          NOT NULL,
    package_name_snapshot           VARCHAR(100)    NOT NULL,
    package_duration_days_snapshot  SMALLINT        NOT NULL,
    package_price_snapshot          DECIMAL(12,2)   NOT NULL,
    status                          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    start_date                      DATE            NULL,
    end_date                        DATE            NULL,
    approved_by_user_id             BIGINT          NULL,
    approved_at                     TIMESTAMP(6)    NULL,
    cancelled_by_user_id            BIGINT          NULL,
    cancelled_at                    TIMESTAMP(6)    NULL,
    version                         BIGINT          NOT NULL DEFAULT 0,
    active_member_key               BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'ACTIVE' THEN member_id ELSE NULL END
    ) STORED,
    pending_member_key              BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN member_id ELSE NULL END
    ) STORED,
    created_at                      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_subscriptions              PRIMARY KEY (id),
    CONSTRAINT fk_member_subscriptions_member       FOREIGN KEY (member_id)             REFERENCES users (id)               ON DELETE RESTRICT,
    CONSTRAINT fk_member_subscriptions_package      FOREIGN KEY (package_id)            REFERENCES membership_packages (id) ON DELETE RESTRICT,
    CONSTRAINT fk_member_subscriptions_approver     FOREIGN KEY (approved_by_user_id)   REFERENCES users (id)               ON DELETE RESTRICT,
    CONSTRAINT fk_member_subscriptions_canceller    FOREIGN KEY (cancelled_by_user_id)  REFERENCES users (id)               ON DELETE RESTRICT,
    CONSTRAINT uk_member_subscriptions_one_active   UNIQUE (active_member_key),
    CONSTRAINT uk_member_subscriptions_one_pending  UNIQUE (pending_member_key),
    CONSTRAINT chk_member_subscriptions_status      CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_member_subscriptions_duration    CHECK (package_duration_days_snapshot BETWEEN 1 AND 3650),
    CONSTRAINT chk_member_subscriptions_price       CHECK (package_price_snapshot >= 0),
    CONSTRAINT ck_member_subscriptions_dates CHECK (
        (start_date IS NULL AND end_date IS NULL)
        OR
        (start_date IS NOT NULL AND end_date IS NOT NULL AND end_date > start_date)
    ),
    CONSTRAINT ck_member_subscriptions_state CHECK (
        (
            status = 'PENDING'
            AND start_date IS NULL
            AND end_date IS NULL
            AND approved_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            status IN ('ACTIVE', 'EXPIRED')
            AND start_date IS NOT NULL
            AND end_date IS NOT NULL
            AND approved_at IS NOT NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            status = 'CANCELLED'
            AND cancelled_at IS NOT NULL
            AND (
                (
                    start_date IS NULL
                    AND end_date IS NULL
                    AND approved_at IS NULL
                )
                OR
                (
                    start_date IS NOT NULL
                    AND end_date IS NOT NULL
                    AND approved_at IS NOT NULL
                )
            )
        )
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE subscription_renewal_requests (
    id                              BIGINT      NOT NULL AUTO_INCREMENT,
    member_id                       BIGINT      NOT NULL,
    active_subscription_id          BIGINT      NOT NULL,
    package_id                      BIGINT      NOT NULL,
    package_duration_days_snapshot  SMALLINT    NOT NULL,
    status                          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    previous_end_date               DATE        NULL,
    new_end_date                    DATE        NULL,
    processed_by_user_id            BIGINT      NULL,
    processed_at                    TIMESTAMP(6) NULL,
    version                         BIGINT      NOT NULL DEFAULT 0,
    pending_subscription_key        BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN active_subscription_id ELSE NULL END
    ) STORED,
    created_at                      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_subscription_renewal_requests                 PRIMARY KEY (id),
    CONSTRAINT fk_subscription_renewal_requests_member          FOREIGN KEY (member_id)              REFERENCES users (id)                 ON DELETE RESTRICT,
    CONSTRAINT fk_subscription_renewal_requests_subscription    FOREIGN KEY (active_subscription_id) REFERENCES member_subscriptions (id)  ON DELETE RESTRICT,
    CONSTRAINT fk_subscription_renewal_requests_package         FOREIGN KEY (package_id)             REFERENCES membership_packages (id)   ON DELETE RESTRICT,
    CONSTRAINT fk_subscription_renewal_requests_processor       FOREIGN KEY (processed_by_user_id)   REFERENCES users (id)                 ON DELETE RESTRICT,
    CONSTRAINT uk_renewal_requests_one_pending                  UNIQUE (pending_subscription_key),
    CONSTRAINT chk_subscription_renewal_requests_status         CHECK (status IN ('PENDING', 'PROCESSED')),
    CONSTRAINT chk_subscription_renewal_requests_duration       CHECK (package_duration_days_snapshot BETWEEN 1 AND 3650),
    CONSTRAINT ck_subscription_renewal_requests_state CHECK (
        (
            status = 'PENDING'
            AND previous_end_date IS NULL
            AND new_end_date IS NULL
            AND processed_at IS NULL
        )
        OR
        (
            status = 'PROCESSED'
            AND previous_end_date IS NOT NULL
            AND new_end_date IS NOT NULL
            AND new_end_date > previous_end_date
            AND processed_at IS NOT NULL
        )
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- 4. NHÓM EXERCISE: exercises và 4 bảng Metadata phụ
-- ==============================================================================

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

-- ----------------------------------------------------------------------------

CREATE TABLE exercise_secondary_muscles (
    exercise_id     BIGINT      NOT NULL,
    muscle_group    VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_exercise_secondary_muscles            PRIMARY KEY (exercise_id, muscle_group),
    CONSTRAINT fk_exercise_secondary_muscles_exercise   FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT chk_exercise_secondary_muscles_value     CHECK (muscle_group IN ('CHEST', 'BACK', 'SHOULDERS', 'ARMS', 'LEGS', 'GLUTES', 'CORE', 'CARDIO', 'FULL_BODY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE exercise_equipment (
    exercise_id BIGINT      NOT NULL,
    equipment   VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_exercise_equipment            PRIMARY KEY (exercise_id, equipment),
    CONSTRAINT fk_exercise_equipment_exercise   FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT chk_exercise_equipment_value     CHECK (equipment IN ('BARBELL', 'DUMBBELL', 'MACHINE', 'CABLE', 'BENCH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE exercise_target_body_regions (
    exercise_id BIGINT      NOT NULL,
    body_region VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_exercise_target_body_regions          PRIMARY KEY (exercise_id, body_region),
    CONSTRAINT fk_exercise_target_body_regions_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT chk_exercise_target_body_regions_value   CHECK (body_region IN ('UPPER_BODY', 'LOWER_BODY', 'CORE', 'FULL_BODY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

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

-- ==============================================================================
-- 5. NHÓM WORKOUT PLAN: workout_plans, workout_days, workout_plan_exercises
-- ==============================================================================

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

-- ----------------------------------------------------------------------------

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

-- ----------------------------------------------------------------------------

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

-- ==============================================================================
-- 6. NHÓM WORKOUT LOG: workout_sessions, workout_logs
-- ==============================================================================

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

-- ----------------------------------------------------------------------------

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

-- ==============================================================================
-- 7. NHÓM PROGRESS: body_progress
-- ==============================================================================

CREATE TABLE body_progress (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    member_id       BIGINT          NOT NULL,
    record_date     DATE            NOT NULL,
    weight_kg       DECIMAL(6,2)    NOT NULL,
    muscle_mass_kg  DECIMAL(6,2)    NULL,
    fat_mass_kg     DECIMAL(6,2)    NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_body_progress             PRIMARY KEY (id),
    CONSTRAINT fk_body_progress_member      FOREIGN KEY (member_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_body_progress_member_date UNIQUE (member_id, record_date),
    CONSTRAINT chk_body_progress_weight     CHECK (weight_kg > 0),
    CONSTRAINT chk_body_progress_muscle_mass CHECK (muscle_mass_kg IS NULL OR muscle_mass_kg > 0),
    CONSTRAINT chk_body_progress_fat_mass   CHECK (fat_mass_kg IS NULL OR fat_mass_kg > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- 8. NHÓM AI/NUTRITION: ai_recommendations, nutrition_meal_suggestions
-- ==============================================================================

CREATE TABLE ai_recommendations (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    member_id               BIGINT      NOT NULL,
    workout_plan_id         BIGINT      NOT NULL,
    recommendation_source   VARCHAR(30) NOT NULL,
    validation_status       VARCHAR(30) NOT NULL DEFAULT 'VALIDATED',
    warning_code            VARCHAR(50) NULL,
    calculated_targets      JSON        NOT NULL,
    ai_suggestion           JSON        NOT NULL,
    created_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ai_recommendations                    PRIMARY KEY (id),
    CONSTRAINT uk_ai_recommendations_workout_plan       UNIQUE (workout_plan_id),
    CONSTRAINT fk_ai_recommendations_member             FOREIGN KEY (member_id)       REFERENCES users (id)         ON DELETE RESTRICT,
    CONSTRAINT fk_ai_recommendations_workout_plan       FOREIGN KEY (workout_plan_id) REFERENCES workout_plans (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_recommendations_source            CHECK (recommendation_source IN ('AI_GENERATED', 'FALLBACK_TEMPLATE')),
    CONSTRAINT chk_ai_recommendations_validation_status CHECK (validation_status IN ('VALIDATED', 'FALLBACK_APPLIED')),
    CONSTRAINT chk_ai_recommendations_warning           CHECK (warning_code IS NULL OR warning_code IN ('AI_TIMEOUT', 'AI_RESPONSE_INVALID')),
    CONSTRAINT ck_ai_recommendations_state CHECK (
        (
            recommendation_source = 'AI_GENERATED'
            AND validation_status = 'VALIDATED'
            AND warning_code IS NULL
        )
        OR
        (
            recommendation_source = 'FALLBACK_TEMPLATE'
            AND validation_status = 'FALLBACK_APPLIED'
            AND warning_code IS NOT NULL
        )
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------

CREATE TABLE nutrition_meal_suggestions (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    recommendation_id   BIGINT      NOT NULL,
    meal_name           VARCHAR(100) NOT NULL,
    time_suggest        CHAR(5)     NOT NULL,
    foods_list          JSON        NOT NULL,
    description         TEXT        NULL,
    meal_order          TINYINT     NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_nutrition_meal_suggestions                PRIMARY KEY (id),
    CONSTRAINT fk_nutrition_meal_suggestions_recommendation FOREIGN KEY (recommendation_id) REFERENCES ai_recommendations (id) ON DELETE CASCADE,
    CONSTRAINT uk_nutrition_meals_recommendation_order      UNIQUE (recommendation_id, meal_order),
    CONSTRAINT chk_nutrition_meal_suggestions_order         CHECK (meal_order > 0),
    CONSTRAINT chk_nutrition_meal_suggestions_time          CHECK (time_suggest REGEXP '^([01][0-9]|2[0-3]):[0-5][0-9]$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- 9. CHỈ MỤC (INDEXES)
-- ==============================================================================

CREATE INDEX idx_users_account_status
    ON users (account_status);

CREATE INDEX idx_user_roles_role_user
    ON user_roles (role_id, user_id);

CREATE INDEX idx_member_subscriptions_member_status_end
    ON member_subscriptions (member_id, status, end_date);

CREATE INDEX idx_member_subscriptions_status_end
    ON member_subscriptions (status, end_date);

CREATE INDEX idx_renewal_requests_member_status
    ON subscription_renewal_requests (member_id, status);

CREATE INDEX idx_renewal_requests_active_subscription_status
    ON subscription_renewal_requests (active_subscription_id, status);

CREATE INDEX idx_exercises_active_difficulty
    ON exercises (is_active, difficulty_level);

CREATE INDEX idx_exercises_primary_muscle_active
    ON exercises (primary_muscle_group, is_active);

CREATE INDEX idx_exercise_equipment_equipment
    ON exercise_equipment (equipment, exercise_id);

CREATE INDEX idx_exercise_contraindications_tag
    ON exercise_contraindication_tags (contraindication_tag, exercise_id);

CREATE INDEX idx_workout_plans_member_status
    ON workout_plans (member_id, status);

CREATE INDEX idx_workout_logs_plan_exercise
    ON workout_logs (workout_plan_exercise_id);

CREATE INDEX idx_workout_logs_exercise_date
    ON workout_logs (exercise_id, log_date);

CREATE INDEX idx_ai_recommendations_member_created
    ON ai_recommendations (member_id, created_at);

-- ==============================================================================
-- 10. DỮ LIỆU HẠT GIỐNG (SEED DATA — CHỈ SEED ROLES TĨNH)
-- ==============================================================================

INSERT INTO roles (name, created_at, updated_at)
VALUES
    ('ROLE_ADMIN',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_MEMBER', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_PT',     CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- ==============================================================================
-- END OF schema-draft.sql
-- ==============================================================================
