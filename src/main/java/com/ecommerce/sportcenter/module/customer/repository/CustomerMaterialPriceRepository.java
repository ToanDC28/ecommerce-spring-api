package com.ecommerce.sportcenter.module.customer.repository;

import com.ecommerce.sportcenter.module.customer.entity.CustomerMaterialPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerMaterialPriceRepository extends JpaRepository<CustomerMaterialPrice, Integer>, JpaSpecificationExecutor<CustomerMaterialPrice> {
    Optional<CustomerMaterialPrice> findByCustomer_IdAndMaterial_Id(Integer customerId, Integer materialId);

    List<CustomerMaterialPrice> findByCustomer_Id(Integer customerId);

    boolean existsByCustomer_IdAndMaterial_Id(Integer customerId, Integer materialId);
}
