package com.ecommerce.sportcenter.module.payroll.service;

import com.ecommerce.sportcenter.module.payroll.dto.request.UpsertAttendanceRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.AttendanceResponse;

import java.util.List;

public interface AttendanceService {
    AttendanceResponse upsert(UpsertAttendanceRequest request);

    List<AttendanceResponse> list(String period, Integer staffId);
}
