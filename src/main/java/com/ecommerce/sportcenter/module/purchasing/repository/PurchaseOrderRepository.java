package com.ecommerce.sportcenter.module.purchasing.repository;

import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer>, JpaSpecificationExecutor<PurchaseOrder> {
    Optional<PurchaseOrder> findByCode(String code);

    boolean existsByCode(String code);
}
