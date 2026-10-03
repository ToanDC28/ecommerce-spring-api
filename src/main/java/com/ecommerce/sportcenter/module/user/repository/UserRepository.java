package com.ecommerce.sportcenter.module.user.repository;

import com.ecommerce.sportcenter.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * Count other enabled users holding the ADMIN role, excluding the given id.
     * Used to prevent disabling/deleting/demoting the last active admin.
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ADMIN' AND u.enabled = true AND u.id <> :excludeId")
    long countOtherEnabledAdmins(@Param("excludeId") int excludeId);
}
