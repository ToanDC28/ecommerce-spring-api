package com.ecommerce.sportcenter.module.payroll.repository;

import com.ecommerce.sportcenter.module.payroll.entity.PayrollSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollSettingRepository extends JpaRepository<PayrollSetting, Integer> {
}
