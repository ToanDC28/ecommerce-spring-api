package com.ecommerce.sportcenter.module.sales.repository;

import com.ecommerce.sportcenter.module.sales.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Integer>, JpaSpecificationExecutor<SalesOrderItem> {
}
