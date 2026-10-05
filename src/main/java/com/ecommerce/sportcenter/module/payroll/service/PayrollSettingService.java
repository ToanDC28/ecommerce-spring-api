package com.ecommerce.sportcenter.module.payroll.service;

import com.ecommerce.sportcenter.module.payroll.dto.request.UpdatePayrollSettingRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollSettingResponse;

public interface PayrollSettingService {
    PayrollSettingResponse get();

    PayrollSettingResponse update(UpdatePayrollSettingRequest request);
}
