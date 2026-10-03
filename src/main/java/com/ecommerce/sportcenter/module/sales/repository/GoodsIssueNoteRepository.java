package com.ecommerce.sportcenter.module.sales.repository;

import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueNote;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsIssueNoteRepository extends JpaRepository<GoodsIssueNote, Integer>, JpaSpecificationExecutor<GoodsIssueNote> {
    Optional<GoodsIssueNote> findByCode(String code);

    boolean existsByCode(String code);

    List<GoodsIssueNote> findBySalesOrder_IdAndStatus(Integer salesOrderId, GoodsIssueStatus status);
}
