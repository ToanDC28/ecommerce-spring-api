package com.ecommerce.sportcenter.module.sales.repository;

import com.ecommerce.sportcenter.module.sales.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Integer>, JpaSpecificationExecutor<SalesOrder> {
    Optional<SalesOrder> findByCode(String code);

    boolean existsByCode(String code);
}
