package com.ecommerce.sportcenter.module.material.repository;

import com.ecommerce.sportcenter.module.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer>, JpaSpecificationExecutor<Material> {
    Optional<Material> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Integer id);
}
