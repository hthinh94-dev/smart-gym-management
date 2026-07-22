package com.thinh.smartgym.auth.repository;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.common.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

}
