package com.ecommerce.sportcenter.module.customer.repository;

import com.ecommerce.sportcenter.module.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Customer> findByPhone(String phone);
}
