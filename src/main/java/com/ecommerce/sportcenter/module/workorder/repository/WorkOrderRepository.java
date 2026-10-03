package com.ecommerce.sportcenter.module.workorder.repository;

import com.ecommerce.sportcenter.module.workorder.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Integer>, JpaSpecificationExecutor<WorkOrder> {
    Optional<WorkOrder> findByCode(String code);

    boolean existsByCode(String code);

    java.util.List<WorkOrder> findByCustomer_Id(Integer customerId);
}
