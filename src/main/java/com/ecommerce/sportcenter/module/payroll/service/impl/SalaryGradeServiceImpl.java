package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.CreateSalaryGradeRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.SalaryGradeResponse;
import com.ecommerce.sportcenter.module.payroll.entity.SalaryGrade;
import com.ecommerce.sportcenter.module.payroll.repository.SalaryGradeRepository;
import com.ecommerce.sportcenter.module.payroll.service.SalaryGradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryGradeServiceImpl implements SalaryGradeService {

    private final SalaryGradeRepository salaryGradeRepository;
    private final PayrollMapper payrollMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SalaryGradeResponse> getAll() {
        log.info("Fetching all salary grades");
        return salaryGradeRepository.findAll().stream().map(payrollMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public SalaryGradeResponse create(CreateSalaryGradeRequest request) {
        String level = request.getLevel().trim().toUpperCase();
        if (salaryGradeRepository.existsByLevel(level)) {
            throw new BusinessValidationException("Salary grade '" + level + "' already exists");
        }
        SalaryGrade grade = SalaryGrade.builder()
                .level(level)
                .baseSalary(request.getBaseSalary())
                .allowance(request.getAllowance() == null ? 0L : request.getAllowance())
                .overtimeRatePerHour(request.getOvertimeRatePerHour())
                .active(true)
                .build();
        return payrollMapper.toResponse(salaryGradeRepository.save(grade));
    }
}
