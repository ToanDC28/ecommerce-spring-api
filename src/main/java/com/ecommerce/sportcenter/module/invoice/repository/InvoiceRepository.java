package com.ecommerce.sportcenter.module.invoice.repository;

import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Khóa bi quan khi ghi nhận payment/refund — chống double-pay đồng thời
     * trên cùng invoice (2 thu ngân thu cùng lúc).
     * Dùng @Query tường minh vì hậu tố ForUpdate không phải từ khóa query hợp lệ.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByRefCodeAndTypeAndStatusNot(String refCode, InvoiceType type, InvoiceStatus status);

    List<Invoice> findByStatusInAndDueDateBefore(List<InvoiceStatus> statuses, LocalDate date);

    List<Invoice> findByWorkOrderId(Integer workOrderId);
}
