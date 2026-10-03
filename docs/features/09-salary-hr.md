# 09 — Salary / HR Management (thợ xưởng + khoán WO)

## Purpose

Pay staff xưởng (thợ cơ khí, phụ việc, kho, kế toán) monthly with attendance and overtime.
Đặc thù xưởng: ngoài lương tháng, thợ có thể hưởng khoán theo WorkOrder (một phần `laborCost` của WO DONE) — quyết toán khoán theo WO, không tính 2 lần vào payroll tháng.

## Current state

DONE phase 1 — `module/payroll/*` (`SalaryGrade`, `Attendance` gắn grade theo tháng, `Payroll` snapshot + generate/approve/reject/pay, controllers `/api/salary-grades` + `/api/attendances` + `/api/payrolls` kèm `/my`, tests Mockito+Nest+AssertJ). Khoán WO theo `laborCost` để phase 2 (`WorkOrderBonus`, tránh double-count — xem Dependencies).

## Data model

`SalaryGrade(id, level [L1..L5], baseSalary, allowance, overtimeRatePerHour)`
`Attendance(staff_id, period [2026-09], workingDays, overtimeHours, leaveDays, note)`
`Payroll(id, period, staff_id, baseSalary, allowance, overtimePay, insuranceDeduction (10.5%), taxDeduction, netPay, status[PENDING,APPROVED,PAID,REJECTED], approvedBy, paidAt)`

Formula: `gross = base + allowance + overtimeHours*overtimeRate`, `net = gross - insurance - tax`.

## Workflow

```
Generate PENDING payrolls for period (from Attendance + SalaryGrade)
 -> APPROVED (ACCOUNTANT/chủ review) -> PAID (cash/bank, creates Payment-like record)
 REJECTED returns to PENDING with note
```

## API

```
GET  /api/salary-grades                                    # PAYROLL_READ
POST /api/salary-grades {level, baseSalary, allowance?, overtimeRatePerHour}  # PAYROLL_WRITE (level UNIQUE)
GET  /api/attendances?period=&staffId=                     # PAYROLL_READ
POST /api/attendances {staffId, period, salaryGradeId, workingDays<=31, overtimeHours, leaveDays, note?}  # PAYROLL_WRITE (upsert theo staff+period)
GET  /api/payrolls?period=&staffId=&status=&page&size      # PAYROLL_READ (thợ chỉ thấy của mình; PAYROLL_WRITE thấy tất cả)
GET  /api/payrolls/my?period=                              # auth (lương của tôi)
GET  /api/payrolls/{id}                                    # PAYROLL_READ (thợ khác xem ké -> 403)
POST /api/payrolls/generate?period=2026-09                 # PAYROLL_WRITE (idempotent: tạo/tính lại PENDING + REJECTED->PENDING, bỏ qua APPROVED/PAID)
POST /api/payrolls/{id}/approve {taxDeduction?, note?}     # PAYROLL_WRITE (chốt thuế TNCN, tính lại net)
POST /api/payrolls/{id}/reject {note?}                     # PAYROLL_WRITE (PENDING/APPROVED -> REJECTED)
POST /api/payrolls/{id}/pay                                # PAYROLL_WRITE (APPROVED -> PAID + paidAt, bất biến)
```

## RBAC

- `PAYROLL_READ`: staff sees own; `ACCOUNTANT/ADMIN` see all.
- `PAYROLL_WRITE`: `ACCOUNTANT, ADMIN`.

## Acceptance criteria

- [x] Regenerate for same period is idempotent (tạo/tính lại PENDING + REJECTED→PENDING, bỏ qua APPROVED/PAID).
- [x] `netPay` computed server-side (`gross = base + allowance + otHours*rate`, `insurance = round(gross*10.5%)`, `net = gross - insurance - tax`); inputs validated (`workingDays/leaveDays <= 31`, period YYYY-MM, grade active, staff enabled).
- [x] Only `APPROVED` can be paid; `PAID` is immutable; `PENDING/REJECTED` mới approve được.
- [x] Thợ chỉ xem được lương của mình (`GET /my`, search + detail scope theo user; xem ké → 403).
- [ ] Khoán WO (`WorkOrderBonus`) + `StaffProfile` — phase 2.

## Dependencies

Needs `StaffProfile` (01 — `user_id` PK/FK: `position` thợ chính/phụ, `hireDate`, `bankAccount`, `salaryGrade_id`, `warehouse_id?`).
Phase 2 thêm `WorkOrderBonus(work_order_id, staff_id, amount)` nếu chia khoán theo WO — khi đó payroll tháng phải loại trừ phần đã khoán để không double-count với `laborCost` trong báo cáo lãi WO (11) và profit (10).
Independent of inventory/sales; feeds cost reports (10).
