package com.ecommerce.sportcenter.module.payroll.service;

import com.ecommerce.sportcenter.module.payroll.dto.request.RecordLeaveRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.LeaveResponse;

import java.time.LocalDate;
import java.util.List;

public interface LeaveService {
    LeaveResponse record(RecordLeaveRequest request);

    void delete(int id);

    List<LeaveResponse> list(Integer staffId, LocalDate from, LocalDate to, String username, boolean canManageAll);
}
