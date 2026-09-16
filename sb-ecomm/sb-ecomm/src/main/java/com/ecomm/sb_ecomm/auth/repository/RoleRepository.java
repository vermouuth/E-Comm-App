package com.ecomm.sb_ecomm.auth.repository;

import com.ecomm.sb_ecomm.auth.model.AppRole;
import com.ecomm.sb_ecomm.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(AppRole roleName);
}
