-- V6__create_ai_nutrition_schema.sql
-- AI/Nutrition schema: ai_recommendations, nutrition_meal_suggestions

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

CREATE INDEX idx_ai_recommendations_member_created
    ON ai_recommendations (member_id, created_at);
