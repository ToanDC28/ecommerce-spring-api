package com.ecommerce.sportcenter.module.payment.repository;

import com.ecommerce.sportcenter.module.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Payment> findByTransactionRef(String transactionRef);

    List<Payment> findByInvoice_IdOrderByIdAsc(Integer invoiceId);
}
