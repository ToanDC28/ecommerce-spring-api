package com.ecommerce.sportcenter.module.role.repository;

import com.ecommerce.sportcenter.module.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer>, JpaSpecificationExecutor<Role> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}
