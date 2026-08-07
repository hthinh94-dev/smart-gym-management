-- Extend profile goals and optional body-composition tracking without rewriting prior migrations.

ALTER TABLE member_profiles
    DROP CHECK chk_member_profiles_goal,
    ADD COLUMN target_weight_kg DECIMAL(6,2) NULL AFTER weight_kg,
    ADD CONSTRAINT chk_member_profiles_goal CHECK (
        fitness_goal IN (
            'BULK', 'CUT', 'MAINTAIN',
            'MUSCLE_GAIN', 'WEIGHT_GAIN', 'FAT_LOSS', 'WEIGHT_LOSS'
        )
    ),
    ADD CONSTRAINT chk_member_profiles_target_weight CHECK (
        target_weight_kg IS NULL OR target_weight_kg > 0
    );

CREATE TABLE member_fitness_goals (
    member_profile_id BIGINT      NOT NULL,
    fitness_goal      VARCHAR(20) NOT NULL,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_fitness_goals PRIMARY KEY (member_profile_id, fitness_goal),
    CONSTRAINT fk_member_fitness_goals_profile
        FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_member_fitness_goals_value CHECK (
        fitness_goal IN (
            'BULK', 'CUT', 'MAINTAIN',
            'MUSCLE_GAIN', 'WEIGHT_GAIN', 'FAT_LOSS', 'WEIGHT_LOSS'
        )
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO member_fitness_goals (member_profile_id, fitness_goal)
SELECT id, fitness_goal
FROM member_profiles;

ALTER TABLE body_progress
    ADD COLUMN muscle_mass_kg DECIMAL(6,2) NULL AFTER weight_kg,
    ADD COLUMN fat_mass_kg    DECIMAL(6,2) NULL AFTER muscle_mass_kg,
    ADD CONSTRAINT chk_body_progress_muscle_mass CHECK (
        muscle_mass_kg IS NULL OR muscle_mass_kg > 0
    ),
    ADD CONSTRAINT chk_body_progress_fat_mass CHECK (
        fat_mass_kg IS NULL OR fat_mass_kg > 0
    );
