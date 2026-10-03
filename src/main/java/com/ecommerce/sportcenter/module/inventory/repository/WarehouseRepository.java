package com.ecommerce.sportcenter.module.inventory.repository;

import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer>, JpaSpecificationExecutor<Warehouse> {
    Optional<Warehouse> findByCode(String code);

    boolean existsByCode(String code);
}
