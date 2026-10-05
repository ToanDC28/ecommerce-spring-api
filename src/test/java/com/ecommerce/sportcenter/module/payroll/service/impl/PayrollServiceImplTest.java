package com.ecommerce.sportcenter.module.payroll.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.payroll.dto.mapper.PayrollMapper;
import com.ecommerce.sportcenter.module.payroll.dto.request.ApprovePayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.request.UpdatePayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollResponse;
import com.ecommerce.sportcenter.module.payroll.entity.Payroll;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollSetting;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus;
import com.ecommerce.sportcenter.module.payroll.entity.SalaryGrade;
import com.ecommerce.sportcenter.module.payroll.entity.StaffLeave;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollRepository;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollSettingRepository;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
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
    private StaffLeaveRepository staffLeaveRepository;
    @Mock
    private PayrollSettingRepository payrollSettingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PayrollMapper payrollMapper;

    @InjectMocks
    private PayrollServiceImpl service;

    private User staff() {
        return User.builder().id(2).username("tho01").email("t@shop.local")
                .password("enc").enabled(true).roles(new HashSet<>())
                .salaryGrade(grade()).build();
    }

    private SalaryGrade grade() {
        return SalaryGrade.builder().id(1).level("L3").baseSalary(8000000L)
                .allowance(1000000L).overtimeRatePerHour(50000L).active(true).build();
    }

    private PayrollSetting setting() {
        return PayrollSetting.builder().id(1).offWeekdays("SUNDAY").standardMonthDays(26).build();
    }

    @Nested
    @DisplayName("generate():")
    class Generate {

        @Test
        @DisplayName("worker tính đúng: nghỉ T7 tính, nghỉ CN bỏ, net chuẩn")
        void math() {
            var staff = staff();
            when(payrollSettingRepository.findAll()).thenReturn(List.of(setting()));
            when(userRepository.findAll()).thenReturn(List.of(staff));
            // Nghỉ CN 06/09 (bỏ vì off-day) + T2 07/09 (tính)
            when(staffLeaveRepository.findByStaff_IdAndLeaveDateBetween(2,
                    LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                    .thenReturn(List.of(
                            StaffLeave.builder().staff(staff).leaveDate(LocalDate.of(2026, 9, 6)).build(),
                            StaffLeave.builder().staff(staff).leaveDate(LocalDate.of(2026, 9, 7)).build()));
            when(payrollRepository.findByStaff_IdAndPeriod(2, "2026-09")).thenReturn(Optional.empty());
            when(payrollRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(payrollMapper.toResponse(any())).thenReturn(PayrollResponse.builder().period("2026-09").build());

            service.generate("2026-09");

            var captor = org.mockito.ArgumentCaptor.forClass(Payroll.class);
            verify(payrollRepository).save(captor.capture());
            Payroll saved = captor.getValue();
            // leaveDays=1 (CN bỏ), leave = round(8M/26*1) = 307692
            // gross = 8M+1M+0 = 9M, insurance = 945000, net = 9M-307692-945000 = 7747308
            assertThat(saved.getLeaveDays()).isEqualTo(1);
            assertThat(saved.getLeaveDeduction()).isEqualTo(307692L);
            assertThat(saved.getNetPay()).isEqualTo(7747308L);
            assertThat(saved.getStatus()).isEqualTo(PayrollStatus.READY_TO_PAY);
        }

        @Test
        @DisplayName("thiếu bậc lương thì báo rõ tên")
        void missingGrade() {
            var noGrade = User.builder().id(3).username("tho02").email("t2@shop.local")
                    .password("enc").enabled(true).roles(new HashSet<>()).build();
            when(payrollSettingRepository.findAll()).thenReturn(List.of(setting()));
            when(userRepository.findAll()).thenReturn(List.of(noGrade));

            assertThatThrownBy(() -> service.generate("2026-09"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("tho02");
        }
    }

    @Nested
    @DisplayName("update() + approve()/pay():")
    class Review {

        @Test
        @DisplayName("update bonus/OT rồi approve chốt thuế, pay flips PAID")
        void flow() {
            var payroll = Payroll.builder().id(1).staff(staff()).period("2026-09")
                    .baseSalary(8000000L).allowance(1000000L).overtimeRate(50000L)
                    .overtimeHours(0.0).overtimePay(0L).grossPay(9000000L)
                    .bonus(0L).leaveDays(1).leaveDeduction(307692L)
                    .insuranceDeduction(945000L).taxDeduction(0L).netPay(7747308L)
                    .status(PayrollStatus.READY_TO_PAY).build();
            when(payrollRepository.findById(1)).thenReturn(Optional.of(payroll));
            when(payrollRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(payrollMapper.toResponse(any())).thenReturn(PayrollResponse.builder().id(1).build());

            service.update(1, UpdatePayrollRequest.builder().bonus(500000L).build());
            // net = 9M + 500k - 307692 - 945000 = 8247308
            assertThat(payroll.getNetPay()).isEqualTo(8247308L);

            service.approve(1, ApprovePayrollRequest.builder().taxDeduction(200000L).build(), "ketoan");
            assertThat(payroll.getNetPay()).isEqualTo(8047308L);
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
                    .status(PayrollStatus.READY_TO_PAY).build();
            when(payrollRepository.findById(1)).thenReturn(Optional.of(payroll));

            assertThatThrownBy(() -> service.getById(1, "tho01", false))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
