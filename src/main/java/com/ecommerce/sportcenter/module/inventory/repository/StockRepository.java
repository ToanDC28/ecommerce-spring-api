package com.ecommerce.sportcenter.module.inventory.repository;

import com.ecommerce.sportcenter.module.inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Integer>, JpaSpecificationExecutor<Stock> {
    Optional<Stock> findByWarehouse_IdAndMaterial_Id(Integer warehouseId, Integer materialId);
}
