-- V3__create_membership_schema.sql
-- Membership schema: membership_packages, member_subscriptions, subscription_renewal_requests

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

CREATE INDEX idx_member_subscriptions_member_status_end
    ON member_subscriptions (member_id, status, end_date);

CREATE INDEX idx_member_subscriptions_status_end
    ON member_subscriptions (status, end_date);

CREATE INDEX idx_renewal_requests_member_status
    ON subscription_renewal_requests (member_id, status);

CREATE INDEX idx_renewal_requests_active_subscription_status
    ON subscription_renewal_requests (active_subscription_id, status);
