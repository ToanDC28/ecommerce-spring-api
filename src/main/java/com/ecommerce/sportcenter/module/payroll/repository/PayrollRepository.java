package com.ecommerce.sportcenter.module.payroll.repository;

import com.ecommerce.sportcenter.module.payroll.entity.Payroll;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Integer>, JpaSpecificationExecutor<Payroll> {
    Optional<Payroll> findByStaff_IdAndPeriod(Integer staffId, String period);

    List<Payroll> findByPeriod(String period);
}
