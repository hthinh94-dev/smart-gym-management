package com.thinh.smartgym.auth.repository;

import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
