package com.ecommerce.sportcenter.module.inventory.repository;

import com.ecommerce.sportcenter.module.inventory.entity.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Integer>, JpaSpecificationExecutor<StockTransaction> {
}
