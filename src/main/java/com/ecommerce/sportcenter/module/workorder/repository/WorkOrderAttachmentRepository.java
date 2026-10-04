package com.ecommerce.sportcenter.module.workorder.repository;

import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderAttachmentRepository extends JpaRepository<WorkOrderAttachment, Integer>, JpaSpecificationExecutor<WorkOrderAttachment> {
    List<WorkOrderAttachment> findByWorkOrder_Id(Integer workOrderId);

    long countByWorkOrder_Id(Integer workOrderId);
}
