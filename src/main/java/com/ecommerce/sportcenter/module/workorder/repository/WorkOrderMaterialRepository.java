package com.ecommerce.sportcenter.module.workorder.repository;

import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderMaterialRepository extends JpaRepository<WorkOrderMaterial, Integer>, JpaSpecificationExecutor<WorkOrderMaterial> {
    List<WorkOrderMaterial> findByWorkOrder_Id(Integer workOrderId);
}
