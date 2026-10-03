package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.UpsertAttendanceRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.AttendanceResponse;
import com.ecommerce.sportcenter.module.payroll.entity.Attendance;
import com.ecommerce.sportcenter.module.payroll.entity.SalaryGrade;
import com.ecommerce.sportcenter.module.payroll.repository.AttendanceRepository;
import com.ecommerce.sportcenter.module.payroll.repository.SalaryGradeRepository;
import com.ecommerce.sportcenter.module.payroll.service.AttendanceService;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SalaryGradeRepository salaryGradeRepository;
    private final PayrollMapper payrollMapper;

    @Override
    @Transactional
    public AttendanceResponse upsert(UpsertAttendanceRequest request) {
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getStaffId()));
        if (!staff.isEnabled()) {
            throw new BusinessValidationException("Cannot record attendance for disabled user '" + staff.getUsername() + "'");
        }
        SalaryGrade grade = salaryGradeRepository.findById(request.getSalaryGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("SalaryGrade not found with id: " + request.getSalaryGradeId()));
        if (!grade.isActive()) {
            throw new BusinessValidationException("Salary grade '" + grade.getLevel() + "' is inactive");
        }
        Attendance attendance = attendanceRepository
                .findByStaff_IdAndPeriod(request.getStaffId(), request.getPeriod())
                .orElseGet(() -> Attendance.builder().staff(staff).period(request.getPeriod()).build());
        attendance.setStaff(staff);
        attendance.setSalaryGrade(grade);
        attendance.setWorkingDays(request.getWorkingDays() == null ? 0 : request.getWorkingDays());
        attendance.setOvertimeHours(request.getOvertimeHours() == null ? 0.0 : request.getOvertimeHours());
        attendance.setLeaveDays(request.getLeaveDays() == null ? 0 : request.getLeaveDays());
        attendance.setNote(request.getNote());
        log.info("Attendance upsert - staff={}, period={}", staff.getUsername(), request.getPeriod());
        return payrollMapper.toResponse(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> list(String period, Integer staffId) {
        Specification<Attendance> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (period != null && !period.isBlank()) {
                predicates.add(builder.equal(root.get("period"), period));
            }
            if (staffId != null) {
                predicates.add(builder.equal(root.get("staff").get("id"), staffId));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return attendanceRepository.findAll(spec).stream().map(payrollMapper::toResponse).toList();
    }
}
