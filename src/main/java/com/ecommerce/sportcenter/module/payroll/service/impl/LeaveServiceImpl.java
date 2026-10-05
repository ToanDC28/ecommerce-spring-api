package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.RecordLeaveRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.LeaveResponse;
import com.ecommerce.sportcenter.module.payroll.entity.StaffLeave;
import com.ecommerce.sportcenter.module.payroll.repository.StaffLeaveRepository;
import com.ecommerce.sportcenter.module.payroll.service.LeaveService;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveServiceImpl implements LeaveService {

    private final StaffLeaveRepository staffLeaveRepository;
    private final UserRepository userRepository;
    private final PayrollMapper payrollMapper;

    @Override
    @Transactional
    public LeaveResponse record(RecordLeaveRequest request) {
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getStaffId()));
        if (!staff.isEnabled()) {
            throw new BusinessValidationException("Cannot record leave for disabled user '" + staff.getUsername() + "'");
        }
        if (staffLeaveRepository.existsByStaff_IdAndLeaveDate(request.getStaffId(), request.getLeaveDate())) {
            throw new BusinessValidationException("Leave already recorded for " + request.getLeaveDate());
        }
        StaffLeave leave = staffLeaveRepository.save(StaffLeave.builder()
                .staff(staff)
                .leaveDate(request.getLeaveDate())
                .note(request.getNote())
                .build());
        log.info("Leave recorded - staff={}, date={}", staff.getUsername(), request.getLeaveDate());
        return payrollMapper.toResponse(leave);
    }

    @Override
    @Transactional
    public void delete(int id) {
        StaffLeave leave = staffLeaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found with id: " + id));
        staffLeaveRepository.delete(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> list(Integer staffId, LocalDate from, LocalDate to, String username, boolean canManageAll) {
        Integer effectiveStaffId = staffId;
        if (!canManageAll) {
            User me = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
            effectiveStaffId = me.getId();
        }
        final Integer sid = effectiveStaffId;
        Specification<StaffLeave> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (sid != null) {
                predicates.add(builder.equal(root.get("staff").get("id"), sid));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("leaveDate"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("leaveDate"), to));
            }
            query.orderBy(builder.desc(root.get("leaveDate")));
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return staffLeaveRepository.findAll(spec).stream().map(payrollMapper::toResponse).toList();
    }
}
