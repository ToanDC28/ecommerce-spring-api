package com.ecommerce.sportcenter.module.payroll.repository;

import com.ecommerce.sportcenter.module.payroll.entity.StaffLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffLeaveRepository extends JpaRepository<StaffLeave, Integer>, JpaSpecificationExecutor<StaffLeave> {
    Optional<StaffLeave> findByStaff_IdAndLeaveDate(Integer staffId, LocalDate leaveDate);

    List<StaffLeave> findByStaff_IdAndLeaveDateBetween(Integer staffId, LocalDate from, LocalDate to);

    boolean existsByStaff_IdAndLeaveDate(Integer staffId, LocalDate leaveDate);
}
