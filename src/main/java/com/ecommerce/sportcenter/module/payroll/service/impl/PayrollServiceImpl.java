package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.ApprovePayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.request.SearchPayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollResponse;
import com.ecommerce.sportcenter.module.payroll.entity.Attendance;
import com.ecommerce.sportcenter.module.payroll.entity.Payroll;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus;
import com.ecommerce.sportcenter.module.payroll.repository.AttendanceRepository;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollRepository;
import com.ecommerce.sportcenter.module.payroll.service.PayrollService;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private static final double INSURANCE_RATE = 0.105; // 10.5% gross

    private final PayrollRepository payrollRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final PayrollMapper payrollMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PayrollResponse> search(SearchPayrollRequest request, String currentUsername, boolean canManageAll) {
        Integer staffId = request.getStaffId();
        if (!canManageAll) {
            // Nhân viên thường chỉ thấy lương của chính mình.
            User me = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));
            staffId = me.getId();
        }
        final Integer effectiveStaffId = staffId;
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<Payroll> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getPeriod() != null && !request.getPeriod().isBlank()) {
                predicates.add(builder.equal(root.get("period"), request.getPeriod()));
            }
            if (effectiveStaffId != null) {
                predicates.add(builder.equal(root.get("staff").get("id"), effectiveStaffId));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<Payroll>().build(request));
        var page = payrollRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(payrollMapper::toResponse).toList();
        return PageResponse.<PayrollResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollResponse getById(int id, String currentUsername, boolean canManageAll) {
        log.info("Get payroll by id - id={}", id);
        Payroll payroll = findOrThrow(id);
        if (!canManageAll && (payroll.getStaff() == null
                || !currentUsername.equals(payroll.getStaff().getUsername()))) {
            throw new org.springframework.security.access.AccessDeniedException("You can only view your own payroll");
        }
        return payrollMapper.toResponse(payroll);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponse> myPayrolls(String username, String period) {
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        Specification<Payroll> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(root.get("staff").get("id"), me.getId()));
            if (period != null && !period.isBlank()) {
                predicates.add(builder.equal(root.get("period"), period));
            }
            return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return payrollRepository.findAll(spec).stream().map(payrollMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public List<PayrollResponse> generate(String period) {
        validatePeriod(period);
        var attendances = attendanceRepository.findByPeriod(period);
        if (attendances.isEmpty()) {
            throw new BusinessValidationException("No attendance records for period " + period);
        }
        var result = new java.util.ArrayList<PayrollResponse>();
        for (var att : attendances) {
            var existing = payrollRepository.findByStaff_IdAndPeriod(att.getStaff().getId(), period);
            if (existing.isPresent()
                    && existing.get().getStatus() != PayrollStatus.PENDING
                    && existing.get().getStatus() != PayrollStatus.REJECTED) {
                // APPROVED/PAID bất biến — regenerate bỏ qua (idempotent).
                result.add(payrollMapper.toResponse(existing.get()));
                continue;
            }
            Payroll payroll = existing.orElseGet(() -> Payroll.builder()
                    .staff(att.getStaff()).period(period).build());
            computeFromAttendance(payroll, att);
            payroll.setStatus(PayrollStatus.PENDING);
            result.add(payrollMapper.toResponse(payrollRepository.save(payroll)));
        }
        log.info("Payrolls generated - period={}, count={}", period, result.size());
        return result;
    }

    @Override
    @Transactional
    public PayrollResponse approve(int id, ApprovePayrollRequest request, String username) {
        Payroll payroll = findOrThrow(id);
        if (payroll.getStatus() != PayrollStatus.PENDING && payroll.getStatus() != PayrollStatus.REJECTED) {
            throw new BusinessValidationException("Only PENDING/REJECTED payroll can be approved (current=" + payroll.getStatus() + ")");
        }
        if (request != null && request.getTaxDeduction() != null) {
            // Kế toán chốt thuế TNCN khi duyệt, tính lại thực nhận.
            payroll.setTaxDeduction(request.getTaxDeduction());
            payroll.setNetPay(payroll.getGrossPay() - payroll.getInsuranceDeduction() - payroll.getTaxDeduction());
            if (request.getNote() != null) {
                payroll.setNote(request.getNote());
            }
        }
        payroll.setStatus(PayrollStatus.APPROVED);
        payroll.setApprovedBy(username);
        return payrollMapper.toResponse(payrollRepository.save(payroll));
    }

    @Override
    @Transactional
    public PayrollResponse reject(int id, String note, String username) {
        Payroll payroll = findOrThrow(id);
        if (payroll.getStatus() != PayrollStatus.PENDING && payroll.getStatus() != PayrollStatus.APPROVED) {
            throw new BusinessValidationException("Only PENDING/APPROVED payroll can be rejected (current=" + payroll.getStatus() + ")");
        }
        payroll.setStatus(PayrollStatus.REJECTED);
        if (note != null) {
            payroll.setNote(note);
        }
        log.info("Payroll rejected - id={}, by={}", id, username);
        return payrollMapper.toResponse(payrollRepository.save(payroll));
    }

    @Override
    @Transactional
    public PayrollResponse pay(int id, String username) {
        Payroll payroll = findOrThrow(id);
        if (payroll.getStatus() != PayrollStatus.APPROVED) {
            throw new BusinessValidationException("Only APPROVED payroll can be paid (current=" + payroll.getStatus() + ")");
        }
        payroll.setStatus(PayrollStatus.PAID);
        payroll.setPaidAt(LocalDate.now());
        log.info("Payroll paid - id={}, net={}, by={}", id, payroll.getNetPay(), username);
        return payrollMapper.toResponse(payrollRepository.save(payroll));
    }

    private void computeFromAttendance(Payroll payroll, Attendance att) {
        var grade = att.getSalaryGrade();
        long overtimePay = Math.round(att.getOvertimeHours() * grade.getOvertimeRatePerHour());
        long gross = grade.getBaseSalary() + grade.getAllowance() + overtimePay;
        long insurance = Math.round(gross * INSURANCE_RATE);
        payroll.setGradeLevel(grade.getLevel());
        payroll.setBaseSalary(grade.getBaseSalary());
        payroll.setAllowance(grade.getAllowance());
        payroll.setOvertimeRate(grade.getOvertimeRatePerHour());
        payroll.setOvertimeHours(att.getOvertimeHours());
        payroll.setOvertimePay(overtimePay);
        payroll.setGrossPay(gross);
        payroll.setInsuranceDeduction(insurance);
        // Giữ tax cũ nếu regenerate (kế toán đã chốt tay thì không ghi đè).
        long net = gross - insurance - payroll.getTaxDeduction();
        payroll.setNetPay(net);
    }

    private void validatePeriod(String period) {
        if (period == null || !period.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new BusinessValidationException("Period must be YYYY-MM");
        }
    }

    private Payroll findOrThrow(int id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found with id: " + id));
    }
}
