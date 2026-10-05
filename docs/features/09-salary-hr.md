# 09 — Salary / HR Management (thợ xưởng + khoán WO)

## Purpose

Pay staff xưởng (thợ cơ khí, phụ việc, kho, kế toán) monthly — KHÔNG chấm công hằng ngày.
Chỉ tracking ngày nghỉ + cấu hình ngày nghỉ hợp lệ; mùng 1 hằng tháng worker tự tính lương, admin review detail rồi chốt chi.
Đặc thù xưởng: ngoài lương tháng, thợ có thể hưởng khoán theo WorkOrder (một phần `laborCost` của WO DONE) — quyết toán khoán theo WO, không tính 2 lần vào payroll tháng.

## Current state

DONE — `module/payroll/*`: `SalaryGrade`, `StaffLeave` (chấm nghỉ từng ngày), `PayrollSetting` singleton (off-days + công chuẩn), `Payroll` (worker mùng 1 → READY_TO_PAY → review/update → APPROVED → PAID), grade + base thỏa thuận nằm trên `User` (lúc tuyển), controllers `/api/salary-grades` + `/api/leaves` + `/api/payrolls` + `/api/payroll-settings`, tests Mockito+Nest+AssertJ. Khoán WO theo `laborCost` để phase 2 (`WorkOrderBonus`, tránh double-count — xem Dependencies).

## Data model

`SalaryGrade(id, level [L1..L5], baseSalary, allowance, overtimeRatePerHour)`
`User.salary_grade_id? + agreedBaseSalary?` — hợp đồng lúc tuyển (null = theo grade; `PUT /api/users/{id}/contract` để nâng lương/đổi bậc)
`StaffLeave(id, staff_id, leave_date UNIQUE/staff, note)` — ngày nghỉ thực tế (kể cả rơi vào off-day, worker tự loại)
`PayrollSetting(id=1, offWeekdays CSV DayOfWeek [mặc định SUNDAY], standardMonthDays [mặc định 26])` — lazy-create khi đọc lần đầu
`Payroll(id, period, staff_id, baseSalary (đã chốt: agreedBase ?? grade), allowance, overtimeRate/Hours/Pay, grossPay, bonus, leaveDays (đã loại off-day), offDays (snapshot cấu hình), leaveDeduction (= round(base/stdDays*leaveDays)), insuranceDeduction (10.5% gross), taxDeduction, netPay, status[READY_TO_PAY,PENDING(tương thích cũ),APPROVED,PAID,REJECTED], approvedBy, paidAt)`

Formula: `gross = base + allowance + overtimeHours*overtimeRate` (chưa gồm bonus), `net = gross + bonus - leaveDeduction - insurance - tax`.

## Workflow

```
Trong tháng: ghi ngày nghỉ khi phát sinh (POST /api/leaves)
  + admin config off-days 1 lần (PUT /api/payroll-settings)
Mùng 1 01:00 worker chạy: tính lương tháng trước cho toàn bộ staff enabled
  (bỏ qua người chưa xếp grade — báo rõ tên để xếp tay)
  -> READY_TO_PAY (chờ review; xem detail: base nguồn nào, leaveDays nào, offDays nào)
  -> admin sửa bonus/OT/thuế nếu cần (PUT /api/payrolls/{id})
  -> APPROVED (chốt thuế TNCN) -> PAID (+ paidAt, bất biến)
  REJECTED + note -> sửa lại -> generate lại tính lại
```

## API

```
GET  /api/salary-grades                                    # PAYROLL_READ
POST /api/salary-grades {level, baseSalary, allowance?, overtimeRatePerHour}  # PAYROLL_WRITE (level UNIQUE)
GET  /api/leaves?staffId=&from=&to=                        # PAYROLL_READ (thợ chỉ thấy của mình)
POST /api/leaves {staffId, leaveDate, note?}               # PAYROLL_WRITE (trùng ngày -> 400)
DELETE /api/leaves/{id}                                    # PAYROLL_WRITE (nhập nhầm ngày)
GET  /api/payroll-settings                                 # PAYROLL_READ
PUT  /api/payroll-settings {offWeekdays?, standardMonthDays?}  # PAYROLL_WRITE (vd SATURDAY,SUNDAY)
PUT  /api/users/{id}/contract {salaryGradeId?, agreedBaseSalary?}  # USER_WRITE (xếp/sửa HĐLĐ)
GET  /api/payrolls?period=&staffId=&status=&page&size      # PAYROLL_READ (thợ chỉ thấy của mình; PAYROLL_WRITE thấy tất cả)
GET  /api/payrolls/my?period=                              # auth (lương của tôi)
GET  /api/payrolls/{id}                                    # PAYROLL_READ (thợ khác xem ké -> 403)
POST /api/payrolls/generate?period=2026-09                 # PAYROLL_WRITE (idempotent: tạo/tính lại READY_TO_PAY (+PENDING/REJECTED cũ), bỏ qua APPROVED/PAID)
PUT  /api/payrolls/{id} {bonus?, overtimeHours?, taxDeduction?, note?}  # PAYROLL_WRITE (review trước duyệt)
POST /api/payrolls/{id}/approve {taxDeduction?, note?}     # PAYROLL_WRITE (chốt thuế TNCN, tính lại net)
POST /api/payrolls/{id}/reject {note?}                     # PAYROLL_WRITE (READY/PENDING/APPROVED -> REJECTED)
POST /api/payrolls/{id}/pay                                # PAYROLL_WRITE (APPROVED -> PAID + paidAt, bất biến)
```

## RBAC

- `PAYROLL_READ`: staff sees own; `ACCOUNTANT/ADMIN` see all.
- `PAYROLL_WRITE`: `ACCOUNTANT, ADMIN`.

## Acceptance criteria

- [x] Ngày nghỉ ghi từng ngày, trùng ngày → 400; off-day (theo setting) không bị trừ lương.
- [x] Worker mùng 1 01:00 tính tháng trước; thiếu grade báo rõ tên, không crash scheduler.
- [x] Regenerate idempotent (tính lại READY/PENDING/REJECTED, giữ bonus/tax đã nhập tay, bỏ qua APPROVED/PAID).
- [x] Detail review đủ căn cứ: base nguồn nào, leaveDays, offDays snapshot, bonus, OT, thuế.
- [x] `netPay` computed server-side; inputs validated (period YYYY-MM, offWeekdays CSV DayOfWeek, stdDays 1..31).
- [x] Only `APPROVED` can be paid; `PAID` immutable; update chỉ khi chưa duyệt xong.
- [x] Thợ chỉ xem được lương/nghỉ của mình (`GET /my`, search + detail scope theo user; xem ké → 403).
- [ ] Khoán WO (`WorkOrderBonus`) + `StaffProfile` đầy đủ — phase 2.

## Dependencies

Needs grade + agreed base trên `User` (xếp lúc tuyển/sửa qua `PUT /users/{id}/contract`).
Phase 2 thêm `WorkOrderBonus(work_order_id, staff_id, amount)` nếu chia khoán theo WO — khi đó payroll tháng phải loại trừ phần đã khoán để không double-count với `laborCost` trong báo cáo lãi WO (11) và profit (10).
Independent of inventory/sales; feeds cost reports (10).
