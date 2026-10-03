package com.ecommerce.sportcenter.module.payroll.repository;

import com.ecommerce.sportcenter.module.payroll.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer>, JpaSpecificationExecutor<Attendance> {
    Optional<Attendance> findByStaff_IdAndPeriod(Integer staffId, String period);

    List<Attendance> findByPeriod(String period);
}
