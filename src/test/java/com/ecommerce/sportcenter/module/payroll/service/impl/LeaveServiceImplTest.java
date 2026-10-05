package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.RecordLeaveRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.LeaveResponse;
import com.ecommerce.sportcenter.module.payroll.entity.StaffLeave;
import com.ecommerce.sportcenter.module.payroll.repository.StaffLeaveRepository;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock
    private StaffLeaveRepository staffLeaveRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PayrollMapper payrollMapper;

    @InjectMocks
    private LeaveServiceImpl service;

    private User staff() {
        return User.builder().id(2).username("tho01").email("t@shop.local")
                .password("enc").enabled(true).roles(new HashSet<>()).build();
    }

    @Nested
    @DisplayName("record():")
    class Record {

        @Test
        @DisplayName("success + reject duplicate day")
        void successAndDuplicate() {
            when(userRepository.findById(2)).thenReturn(Optional.of(staff()));
            when(staffLeaveRepository.existsByStaff_IdAndLeaveDate(2, LocalDate.of(2026, 9, 15)))
                    .thenReturn(false).thenReturn(true);
            when(staffLeaveRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(payrollMapper.toResponse(any(StaffLeave.class)))
                    .thenReturn(LeaveResponse.builder().id(1).build());

            var req = RecordLeaveRequest.builder().staffId(2)
                    .leaveDate(LocalDate.of(2026, 9, 15)).note("Việc gia đình").build();
            assertThat(service.record(req).getId()).isEqualTo(1);

            assertThatThrownBy(() -> service.record(req))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("already recorded");
        }

        @Test
        @DisplayName("reject disabled staff")
        void disabled() {
            var off = staff();
            off.setEnabled(false);
            when(userRepository.findById(2)).thenReturn(Optional.of(off));

            assertThatThrownBy(() -> service.record(RecordLeaveRequest.builder()
                    .staffId(2).leaveDate(LocalDate.now()).build()))
                    .isInstanceOf(BusinessValidationException.class);
            verify(staffLeaveRepository, never()).save(any());
        }
    }
}
