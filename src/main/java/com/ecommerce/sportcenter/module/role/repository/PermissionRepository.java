package com.ecommerce.sportcenter.module.role.repository;

import com.ecommerce.sportcenter.module.role.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer>, JpaSpecificationExecutor<Permission> {
    Optional<Permission> findByName(String name);
}
