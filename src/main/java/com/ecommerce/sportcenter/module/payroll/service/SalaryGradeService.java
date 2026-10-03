package com.ecommerce.sportcenter.module.payroll.service;

import com.ecommerce.sportcenter.module.payroll.dto.request.CreateSalaryGradeRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.SalaryGradeResponse;

import java.util.List;

public interface SalaryGradeService {
    List<SalaryGradeResponse> getAll();

    SalaryGradeResponse create(CreateSalaryGradeRequest request);
}
