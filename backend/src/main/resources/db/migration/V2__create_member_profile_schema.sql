-- V2__create_member_profile_schema.sql
-- Member profile schema: member_profiles and 5 collection tables

CREATE TABLE member_profiles (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT          NOT NULL,
    gender                  VARCHAR(10)     NOT NULL,
    date_of_birth           DATE            NOT NULL,
    height_cm               DECIMAL(5,2)    NOT NULL,
    weight_kg               DECIMAL(6,2)    NOT NULL,
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
    CONSTRAINT chk_member_profiles_goal         CHECK (fitness_goal IN ('BULK', 'CUT', 'MAINTAIN')),
    CONSTRAINT chk_member_profiles_level        CHECK (fitness_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_member_profiles_activity     CHECK (activity_level IN ('SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE')),
    CONSTRAINT chk_member_profiles_days         CHECK (workout_days_per_week BETWEEN 1 AND 7),
    CONSTRAINT chk_member_profiles_session      CHECK (max_session_minutes > 0),
    CONSTRAINT chk_member_profiles_dietary      CHECK (dietary_preference IN ('OMNIVORE', 'VEGETARIAN', 'VEGAN')),
    CONSTRAINT chk_member_profiles_meals        CHECK (meals_per_day BETWEEN 1 AND 6),
    CONSTRAINT chk_member_profiles_height       CHECK (height_cm > 0),
    CONSTRAINT chk_member_profiles_weight       CHECK (weight_kg > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE member_available_equipment (
    member_profile_id   BIGINT      NOT NULL,
    equipment           VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_available_equipment        PRIMARY KEY (member_profile_id, equipment),
    CONSTRAINT fk_member_available_equipment_profile FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_member_available_equipment_value CHECK (equipment IN ('BARBELL', 'DUMBBELL', 'MACHINE', 'CABLE', 'BENCH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE member_target_muscle_groups (
    member_profile_id   BIGINT      NOT NULL,
    muscle_group        VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_target_muscle_groups           PRIMARY KEY (member_profile_id, muscle_group),
    CONSTRAINT fk_member_target_muscle_groups_profile   FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_member_target_muscle_groups_value    CHECK (muscle_group IN ('CHEST', 'BACK', 'SHOULDERS', 'ARMS', 'LEGS', 'GLUTES', 'CORE', 'CARDIO', 'FULL_BODY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE member_food_allergies (
    member_profile_id   BIGINT      NOT NULL,
    allergy_name        VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_food_allergies             PRIMARY KEY (member_profile_id, allergy_name),
    CONSTRAINT fk_member_food_allergies_profile     FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE member_excluded_foods (
    member_profile_id   BIGINT      NOT NULL,
    food_name           VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_excluded_foods             PRIMARY KEY (member_profile_id, food_name),
    CONSTRAINT fk_member_excluded_foods_profile     FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
