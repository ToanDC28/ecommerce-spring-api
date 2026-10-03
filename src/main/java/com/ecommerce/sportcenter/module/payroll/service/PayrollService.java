package com.ecommerce.sportcenter.module.payroll.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.payroll.dto.request.ApprovePayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.request.SearchPayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollResponse;

import java.util.List;

public interface PayrollService {
    PageResponse<PayrollResponse> search(SearchPayrollRequest request, String currentUsername, boolean canManageAll);

    PayrollResponse getById(int id, String currentUsername, boolean canManageAll);

    List<PayrollResponse> myPayrolls(String username, String period);

    List<PayrollResponse> generate(String period);

    PayrollResponse approve(int id, ApprovePayrollRequest request, String username);

    PayrollResponse reject(int id, String note, String username);

    PayrollResponse pay(int id, String username);
}
