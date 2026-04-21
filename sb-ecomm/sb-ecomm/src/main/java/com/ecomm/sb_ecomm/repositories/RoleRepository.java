package com.ecomm.sb_ecomm.repositories;

import com.ecomm.sb_ecomm.models.AppRole;
import com.ecomm.sb_ecomm.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(AppRole roleName);
}
