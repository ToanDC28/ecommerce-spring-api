package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.ApprovePayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollResponse;
import com.ecommerce.sportcenter.module.payroll.entity.Attendance;
import com.ecommerce.sportcenter.module.payroll.entity.Payroll;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus;
import com.ecommerce.sportcenter.module.payroll.entity.SalaryGrade;
import com.ecommerce.sportcenter.module.payroll.repository.AttendanceRepository;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollRepository;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceImplTest {

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PayrollMapper payrollMapper;

    @InjectMocks
    private PayrollServiceImpl service;

    private User staff() {
        return User.builder().id(2).username("tho01").email("t@shop.local")
                .password("enc").enabled(true).roles(new HashSet<>()).build();
    }

    private SalaryGrade grade() {
        return SalaryGrade.builder().id(1).level("L3").baseSalary(8000000L)
                .allowance(1000000L).overtimeRatePerHour(50000L).active(true).build();
    }

    @Nested
    @DisplayName("generate():")
    class Generate {

        @Test
        @DisplayName("success math: gross - 10.5% insurance = net")
        void math() {
            var staff = staff();
            var att = Attendance.builder().id(1).staff(staff).period("2026-09")
                    .salaryGrade(grade()).workingDays(26).overtimeHours(10.0).leaveDays(0).build();
            when(attendanceRepository.findByPeriod("2026-09")).thenReturn(List.of(att));
            when(payrollRepository.findByStaff_IdAndPeriod(2, "2026-09")).thenReturn(Optional.empty());
            when(payrollRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(payrollMapper.toResponse(any())).thenReturn(PayrollResponse.builder().period("2026-09").build());

            service.generate("2026-09");

            var captor = org.mockito.ArgumentCaptor.forClass(Payroll.class);
            verify(payrollRepository).save(captor.capture());
            Payroll saved = captor.getValue();
            // gross = 8M + 1M + 10*50k = 9.5M; insurance = round(9.5M*10.5%) = 997500; net = 8502500
            assertThat(saved.getGrossPay()).isEqualTo(9500000L);
            assertThat(saved.getInsuranceDeduction()).isEqualTo(997500L);
            assertThat(saved.getNetPay()).isEqualTo(8502500L);
            assertThat(saved.getStatus()).isEqualTo(PayrollStatus.PENDING);
        }

        @Test
        @DisplayName("idempotent: skip APPROVED/PAID, recalc PENDING")
        void idempotent() {
            var staff = staff();
            var att = Attendance.builder().id(1).staff(staff).period("2026-09")
                    .salaryGrade(grade()).workingDays(26).overtimeHours(0.0).leaveDays(0).build();
            var paid = Payroll.builder().id(9).staff(staff).period("2026-09")
                    .grossPay(1L).netPay(1L).status(PayrollStatus.PAID).build();
            when(attendanceRepository.findByPeriod("2026-09")).thenReturn(List.of(att));
            when(payrollRepository.findByStaff_IdAndPeriod(2, "2026-09")).thenReturn(Optional.of(paid));
            when(payrollMapper.toResponse(paid)).thenReturn(PayrollResponse.builder().id(9).build());

            var result = service.generate("2026-09");

            assertThat(result).hasSize(1);
            verify(payrollRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject bad period format")
        void badPeriod() {
            assertThatThrownBy(() -> service.generate("2026-13"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("YYYY-MM");
        }
    }

    @Nested
    @DisplayName("approve()/pay():")
    class ApprovePay {

        @Test
        @DisplayName("approve chốt thuế, pay flips PAID immutable")
        void flow() {
            var payroll = Payroll.builder().id(1).staff(staff()).period("2026-09")
                    .grossPay(9500000L).insuranceDeduction(997500L).taxDeduction(0L)
                    .netPay(8502500L).status(PayrollStatus.PENDING).build();
            when(payrollRepository.findById(1)).thenReturn(Optional.of(payroll));
            when(payrollRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(payrollMapper.toResponse(any())).thenReturn(PayrollResponse.builder().id(1).build());

            service.approve(1, ApprovePayrollRequest.builder().taxDeduction(500000L).build(), "ketoan");
            // net = 9.5M - 997500 - 500k = 8002500
            assertThat(payroll.getNetPay()).isEqualTo(8002500L);
            assertThat(payroll.getStatus()).isEqualTo(PayrollStatus.APPROVED);

            service.pay(1, "ketoan");
            assertThat(payroll.getStatus()).isEqualTo(PayrollStatus.PAID);
            assertThat(payroll.getPaidAt()).isNotNull();

            assertThatThrownBy(() -> service.pay(1, "ketoan"))
                    .isInstanceOf(BusinessValidationException.class);
        }
    }

    @Nested
    @DisplayName("getById():")
    class GetById {

        @Test
        @DisplayName("staff khác không xem được phiếu của người khác")
        void forbidden() {
            var other = User.builder().id(3).username("tho02").enabled(true).roles(new HashSet<>()).build();
            var payroll = Payroll.builder().id(1).staff(other).period("2026-09")
                    .status(PayrollStatus.PENDING).build();
            when(payrollRepository.findById(1)).thenReturn(Optional.of(payroll));

            assertThatThrownBy(() -> service.getById(1, "tho01", false))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
