package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.UpdatePayrollSettingRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollSettingResponse;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollSetting;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollSettingRepository;
import com.ecommerce.sportcenter.module.payroll.service.PayrollSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollSettingServiceImpl implements PayrollSettingService {

    private final PayrollSettingRepository payrollSettingRepository;
    private final PayrollMapper payrollMapper;

    @Override
    @Transactional(readOnly = true)
    public PayrollSettingResponse get() {
        return payrollMapper.toResponse(current());
    }

    @Override
    @Transactional
    public PayrollSettingResponse update(UpdatePayrollSettingRequest request) {
        PayrollSetting setting = current();
        if (request.getOffWeekdays() != null) {
            // Chuẩn hóa: uppercase, bỏ khoảng trắng (validate format ở DTO).
            setting.setOffWeekdays(request.getOffWeekdays().toUpperCase().replaceAll("\\s+", ""));
        }
        if (request.getStandardMonthDays() != null) {
            setting.setStandardMonthDays(request.getStandardMonthDays());
        }
        log.info("Payroll setting updated - off={}, stdDays={}", setting.getOffWeekdays(), setting.getStandardMonthDays());
        return payrollMapper.toResponse(payrollSettingRepository.save(setting));
    }

    private PayrollSetting current() {
        return payrollSettingRepository.findAll().stream().findFirst()
                .orElseGet(() -> payrollSettingRepository.save(PayrollSetting.builder().build()));
    }
}
