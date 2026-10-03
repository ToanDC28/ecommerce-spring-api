package com.ecommerce.sportcenter.module.material.repository;

import com.ecommerce.sportcenter.module.material.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByCode(String code);

    boolean existsByCode(String code);
}
