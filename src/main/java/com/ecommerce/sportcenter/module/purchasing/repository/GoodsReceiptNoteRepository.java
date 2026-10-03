package com.ecommerce.sportcenter.module.purchasing.repository;

import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptNote;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Integer>, JpaSpecificationExecutor<GoodsReceiptNote> {
    Optional<GoodsReceiptNote> findByCode(String code);

    boolean existsByCode(String code);

    List<GoodsReceiptNote> findByPurchaseOrder_IdAndStatus(Integer purchaseOrderId, GoodsReceiptStatus status);
}
