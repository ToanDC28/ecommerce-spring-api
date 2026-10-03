package com.ecommerce.sportcenter.module.payroll.repository;

import com.ecommerce.sportcenter.module.payroll.entity.SalaryGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryGradeRepository extends JpaRepository<SalaryGrade, Integer>, JpaSpecificationExecutor<SalaryGrade> {
    Optional<SalaryGrade> findByLevel(String level);

    boolean existsByLevel(String level);
}
