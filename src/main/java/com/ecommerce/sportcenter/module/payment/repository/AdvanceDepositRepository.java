package com.ecommerce.sportcenter.module.payment.repository;

import com.ecommerce.sportcenter.module.payment.entity.AdvanceDeposit;
import com.ecommerce.sportcenter.module.payment.entity.AdvanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvanceDepositRepository extends JpaRepository<AdvanceDeposit, Integer>, JpaSpecificationExecutor<AdvanceDeposit> {
    Optional<AdvanceDeposit> findByCode(String code);

    boolean existsByCode(String code);

    List<AdvanceDeposit> findByWorkOrderIdAndStatusOrderByIdAsc(Integer workOrderId, AdvanceStatus status);

    List<AdvanceDeposit> findBySalesOrderIdAndStatusOrderByIdAsc(Integer salesOrderId, AdvanceStatus status);
}
