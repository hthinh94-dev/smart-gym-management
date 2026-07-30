package com.thinh.smartgym.auth.repository;

import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.entity.UserRoleId;
import com.thinh.smartgym.common.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("""
        SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END
        FROM UserRole ur
        WHERE ur.user.id = :userId AND ur.role.name = :roleName
        """)
    boolean existsByUserIdAndRoleName(
            @Param("userId") Long userId,
            @Param("roleName") RoleName roleName
    );
}
