package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.ApprovePayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.request.SearchPayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.request.UpdatePayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollResponse;
import com.ecommerce.sportcenter.module.payroll.entity.Payroll;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollSetting;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollRepository;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollSettingRepository;
import com.ecommerce.sportcenter.module.payroll.repository.StaffLeaveRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private static final double INSURANCE_RATE = 0.105; // 10.5% gross (chưa gồm bonus)

    private final PayrollRepository payrollRepository;
    private final StaffLeaveRepository staffLeaveRepository;
    private final PayrollSettingRepository payrollSettingRepository;
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
        PayrollSetting setting = getSetting();
        Set<DayOfWeek> offDays = parseOffDays(setting.getOffWeekdays());
        YearMonth ym = YearMonth.parse(period);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        var staffs = userRepository.findAll().stream().filter(User::isEnabled).toList();
        if (staffs.isEmpty()) {
            throw new BusinessValidationException("No enabled staff for period " + period);
        }
        var missingGrade = new java.util.ArrayList<String>();
        var result = new java.util.ArrayList<PayrollResponse>();
        for (var staff : staffs) {
            if (staff.getSalaryGrade() == null) {
                missingGrade.add(staff.getUsername());
                continue;
            }
            var existing = payrollRepository.findByStaff_IdAndPeriod(staff.getId(), period);
            if (existing.isPresent()
                    && existing.get().getStatus() != PayrollStatus.READY_TO_PAY
                    && existing.get().getStatus() != PayrollStatus.PENDING
                    && existing.get().getStatus() != PayrollStatus.REJECTED) {
                // APPROVED/PAID bất biến — regenerate bỏ qua (idempotent).
                result.add(payrollMapper.toResponse(existing.get()));
                continue;
            }
            Payroll payroll = existing.orElseGet(() -> Payroll.builder()
                    .staff(staff).period(period).build());
            computePayroll(payroll, staff, period, from, to, offDays, setting.getStandardMonthDays());
            payroll.setStatus(PayrollStatus.READY_TO_PAY);
            result.add(payrollMapper.toResponse(payrollRepository.save(payroll)));
        }
        if (!missingGrade.isEmpty()) {
            throw new BusinessValidationException("Thiếu bậc lương (xếp ở hồ sơ nhân sự) cho: "
                    + String.join(", ", missingGrade));
        }
        log.info("Payrolls generated - period={}, count={}", period, result.size());
        return result;
    }

    @Override
    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 1 * *")
    public void runMonthlyPayroll() {
        // 01:00 ngày mùng 1 hằng tháng: tính lương tháng vừa xong.
        String period = YearMonth.now().minusMonths(1).toString();
        log.info("Monthly payroll worker - period={}", period);
        try {
            generate(period);
        } catch (BusinessValidationException e) {
            // Thiếu bậc lương...: log để admin xử lý tay, không crash scheduler.
            log.warn("Monthly payroll skipped - period={}, reason={}", period, e.getMessage());
        }
    }

    @Override
    @Transactional
    public PayrollResponse update(int id, UpdatePayrollRequest request) {
        Payroll payroll = findOrThrow(id);
        if (payroll.getStatus() != PayrollStatus.READY_TO_PAY
                && payroll.getStatus() != PayrollStatus.PENDING
                && payroll.getStatus() != PayrollStatus.REJECTED) {
            throw new BusinessValidationException("Only READY_TO_PAY/PENDING/REJECTED payroll can be updated (current=" + payroll.getStatus() + ")");
        }
        if (request.getBonus() != null) {
            payroll.setBonus(request.getBonus());
        }
        if (request.getOvertimeHours() != null) {
            payroll.setOvertimeHours(request.getOvertimeHours());
            payroll.setOvertimePay(Math.round(request.getOvertimeHours() * payroll.getOvertimeRate()));
            payroll.setGrossPay(payroll.getBaseSalary() + payroll.getAllowance() + payroll.getOvertimePay());
            payroll.setInsuranceDeduction(Math.round(payroll.getGrossPay() * INSURANCE_RATE));
        }
        if (request.getTaxDeduction() != null) {
            payroll.setTaxDeduction(request.getTaxDeduction());
        }
        if (request.getNote() != null) {
            payroll.setNote(request.getNote());
        }
        payroll.setNetPay(payroll.getGrossPay() + payroll.getBonus()
                - payroll.getLeaveDeduction() - payroll.getInsuranceDeduction() - payroll.getTaxDeduction());
        return payrollMapper.toResponse(payrollRepository.save(payroll));
    }

    @Override
    @Transactional
    public PayrollResponse approve(int id, ApprovePayrollRequest request, String username) {
        Payroll payroll = findOrThrow(id);
        if (payroll.getStatus() != PayrollStatus.READY_TO_PAY
                && payroll.getStatus() != PayrollStatus.PENDING
                && payroll.getStatus() != PayrollStatus.REJECTED) {
            throw new BusinessValidationException("Only READY_TO_PAY/PENDING/REJECTED payroll can be approved (current=" + payroll.getStatus() + ")");
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
        if (payroll.getStatus() != PayrollStatus.READY_TO_PAY
                && payroll.getStatus() != PayrollStatus.PENDING
                && payroll.getStatus() != PayrollStatus.APPROVED) {
            throw new BusinessValidationException("Only READY_TO_PAY/PENDING/APPROVED payroll can be rejected (current=" + payroll.getStatus() + ")");
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

    private void computePayroll(Payroll payroll, User staff, String period,
                                LocalDate from, LocalDate to, Set<DayOfWeek> offDays, int standardDays) {
        var grade = staff.getSalaryGrade();
        // Lương cơ bản đã thỏa thuận theo HĐLĐ, trống thì lấy theo grade.
        long base = staff.getAgreedBaseSalary() != null ? staff.getAgreedBaseSalary() : grade.getBaseSalary();
        // Ngày nghỉ trong tháng TRỪ ngày nghỉ hợp lệ (cuối tuần theo cấu hình).
        var leaves = staffLeaveRepository.findByStaff_IdAndLeaveDateBetween(staff.getId(), from, to);
        int leaveDays = (int) leaves.stream()
                .map(com.ecommerce.sportcenter.module.payroll.entity.StaffLeave::getLeaveDate)
                .filter(d -> !offDays.contains(d.getDayOfWeek()))
                .count();
        double overtimeHours = payroll.getOvertimeHours(); // worker để 0, admin sửa tay khi review
        long overtimePay = Math.round(overtimeHours * grade.getOvertimeRatePerHour());
        long gross = base + grade.getAllowance() + overtimePay;
        long insurance = Math.round(gross * INSURANCE_RATE);
        long leaveDeduction = Math.round((double) base / standardDays * leaveDays);
        payroll.setGradeLevel(grade.getLevel());
        payroll.setBaseSalary(base);
        payroll.setAllowance(grade.getAllowance());
        payroll.setOvertimeRate(grade.getOvertimeRatePerHour());
        payroll.setOvertimeHours(overtimeHours);
        payroll.setOvertimePay(overtimePay);
        payroll.setGrossPay(gross);
        payroll.setLeaveDays(leaveDays);
        payroll.setOffDays(offDays.stream().map(DayOfWeek::name).sorted().collect(Collectors.joining(",")));
        payroll.setLeaveDeduction(leaveDeduction);
        payroll.setInsuranceDeduction(insurance);
        // Giữ bonus/tax cũ nếu regenerate (đã nhập tay thì không ghi đè).
        long net = gross + payroll.getBonus() - leaveDeduction - insurance - payroll.getTaxDeduction();
        payroll.setNetPay(net);
    }

    private void validatePeriod(String period) {
        if (period == null || !period.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new BusinessValidationException("Period must be YYYY-MM");
        }
    }

    private PayrollSetting getSetting() {
        return payrollSettingRepository.findAll().stream().findFirst()
                .orElseGet(() -> payrollSettingRepository.save(PayrollSetting.builder().build()));
    }

    private Set<DayOfWeek> parseOffDays(String csv) {
        if (csv == null || csv.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> DayOfWeek.valueOf(s.toUpperCase()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    private Payroll findOrThrow(int id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found with id: " + id));
    }
}
