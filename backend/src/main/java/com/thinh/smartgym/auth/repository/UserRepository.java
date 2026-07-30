package com.thinh.smartgym.auth.repository;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.projection.AdminUserProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.userRoles ur
        LEFT JOIN FETCH ur.role
        WHERE LOWER(u.email) = LOWER(:email)
        """)
    Optional<User> findByEmailWithRolesIgnoreCase(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Query(
            value = """
                SELECT
                    u.id AS id,
                    u.full_name AS fullName,
                    u.email AS email,
                    CASE
                        WHEN EXISTS (
                            SELECT 1 FROM user_roles ura
                            JOIN roles ra ON ra.id = ura.role_id
                            WHERE ura.user_id = u.id AND ra.name = 'ROLE_ADMIN'
                        ) THEN 'ROLE_ADMIN'
                        WHEN EXISTS (
                            SELECT 1 FROM user_roles urm
                            JOIN roles rm ON rm.id = urm.role_id
                            WHERE urm.user_id = u.id AND rm.name = 'ROLE_MEMBER'
                        ) THEN 'ROLE_MEMBER'
                        ELSE 'ROLE_PT'
                    END AS role,
                    u.account_status AS accountStatus,
                    u.created_at AS createdAt,
                    CASE WHEN EXISTS (
                        SELECT 1
                        FROM member_subscriptions ms
                        WHERE ms.member_id = u.id
                          AND ms.status = 'ACTIVE'
                          AND ms.start_date <= :today
                          AND :today < ms.end_date
                    ) THEN TRUE ELSE FALSE END AS hasActiveSubscription
                FROM users u
                WHERE (:role IS NULL OR EXISTS (
                    SELECT 1 FROM user_roles urf
                    JOIN roles rf ON rf.id = urf.role_id
                    WHERE urf.user_id = u.id AND rf.name = :role
                ))
                  AND (:status IS NULL OR u.account_status = :status)
                  AND (:search IS NULL
                       OR LOWER(u.full_name) LIKE CONCAT('%', :search, '%')
                       OR LOWER(u.email) LIKE CONCAT('%', :search, '%'))
                ORDER BY u.created_at DESC, u.id DESC
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM users u
                WHERE (:role IS NULL OR EXISTS (
                    SELECT 1 FROM user_roles urf
                    JOIN roles rf ON rf.id = urf.role_id
                    WHERE urf.user_id = u.id AND rf.name = :role
                ))
                  AND (:status IS NULL OR u.account_status = :status)
                  AND (:search IS NULL
                       OR LOWER(u.full_name) LIKE CONCAT('%', :search, '%')
                       OR LOWER(u.email) LIKE CONCAT('%', :search, '%'))
                """,
            nativeQuery = true
    )
    Page<AdminUserProjection> findAdminUsers(
            @Param("role") String role,
            @Param("status") String status,
            @Param("search") String search,
            @Param("today") java.time.LocalDate today,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT COUNT(*)
                FROM member_subscriptions ms
                WHERE ms.member_id = :userId
                  AND ms.status = 'ACTIVE'
                  AND ms.start_date <= :today
                  AND :today < ms.end_date
                """,
            nativeQuery = true
    )
    long countActiveSubscriptions(
            @Param("userId") Long userId,
            @Param("today") java.time.LocalDate today
    );

}
