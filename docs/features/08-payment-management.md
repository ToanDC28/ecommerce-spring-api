# 08 — Payment Management (thu WORK/SALES, chi PURCHASE)

## Purpose

Thu tiền Invoice WORK (hợp đồng sửa/làm mới) + SALES (bán vật liệu), chi tiền Invoice PURCHASE (nhập NCC). Supports cash, bank.

## Current state

DONE phase 1 — `module/payment/*` (`Payment` + `PaymentMethod/Status`, `PaymentService` thu nhiều lần + refund + summary, controllers `/api/invoices/{id}/payments` + `/api/payments`, tests Mockito+Nest+AssertJ).

## Data model

`Payment(id, code UNIQUE [PAY-2026-0001], invoice_id (WORK/SALES/PURCHASE), amount, method[CASH,BANK_TRANSFER], paymentDate, status[SUCCESS,FAILED,REFUNDED], transactionRef? (mã CK ngân hàng, trống với tiền mặt), receivedBy (NV thu tiền — bắt buộc với CASH), note)`
Invoice update on `SUCCESS`: `paidAmount += amount`; `PAID` if `paidAmount >= grandTotal` else `PARTIAL`. Khách hợp đồng (WORK) có thể cọc trước (PARTIAL) rồi thanh toán nốt khi DONE/INVOICED.

Trả 1 phần + ghi nợ (nghiệp vụ chính tiệm): khách được trả nhiều lần trên cùng invoice — mỗi lần thu là 1 Payment row, `paidAmount` cộng dồn, còn nợ = `grandTotal - paidAmount`. Invoice ở `PARTIAL` tới khi trả hết → `PAID`; quá `dueDate` vẫn còn nợ → `OVERDUE` (job hằng đêm ở 07). Dư nợ theo khách xem ở `GET /api/reports/customer-debt` (ưu tiên mã KH ở 12, fallback snapshot) — phase 2 gom Customer master (ĐÃ XONG ở 12).

Cọc trước (`AdvanceDeposit`, DONE): ghi `POST /api/advances {workOrderId XOR salesOrderId, amount, method}` từ màn chi tiết đơn (customer suy ra từ đơn, UI tạo khách trước nếu chưa có) → khi xuất invoice WORK/SALES, cọc ACTIVE của đúng đơn **tự cấn nguyên cục** như 1 lần trả tiền (cọc nào lớn hơn số còn nợ thì giữ lại); `POST /api/advances/{id}/apply {invoiceId}` để cấn tay khi cần (cùng quy tắc). Hủy cọc chỉ khi `ACTIVE`.

Tiền mặt (CASH — luồng chính tiệm nhỏ): thu tại quầy, ghi `receivedBy` + `note` (vd "khách trả đủ", "thối lại 50k"); không cần `transactionRef`. Cuối ngày đối soát theo báo cáo `GET /api/payments/summary` (tổng CASH/BANK theo ngày).

## API

```
POST /api/invoices/{id}/payments {amount, method, transactionRef?, note?}  # PAYMENT_MANAGE (CASH: receivedBy = user hiện tại; BANK: bắt buộc transactionRef)
GET  /api/invoices/{id}/payments          # PAYMENT_MANAGE / INVOICE_READ
GET  /api/payments?invoiceId=&method=&status=&from=&to=&page&size
GET  /api/payments/summary?from=&to=      # đối soát cuối ngày: tổng CASH/BANK theo ngày — DONE
POST /api/payments/{id}/refund            # PAYMENT_MANAGE (SUCCESS only; trừ paidAmount, tính lại PAID/PARTIAL/ISSUED/REFUNDED) — DONE
```

## RBAC

- `PAYMENT_MANAGE`: `ACCOUNTANT, ADMIN` — thu/refund. App nội bộ, không có tài khoản CLIENT nên không có endpoint thanh toán public — mọi thu/chi do staff ghi nhận.
- Đọc (`GET` list/summary/invoice-payments): `PAYMENT_MANAGE` hoặc `INVOICE_READ` (thợ/sales xem được lịch sử thu của invoice mình liên quan).

## Rules

- `amount > 0`, `paidAmount + amount <= grandTotal` (overpay rejected unless credit flag).
- `FAILED` payments don't touch invoice. `REFUND` creates negative payment + sets invoice `REFUNDED` if full.
- All inside `@Transactional` with pessimistic lock on `Invoice`.
- Khách hợp đồng (WORK) thường cọc trước 30-50% khi CONFIRM, thanh toán nốt khi DONE/INVOICED — mỗi lần cọc/thu đều là 1 Payment row.

## Acceptance criteria

- [x] Partial payment flips `ISSUED -> PARTIAL`; full flips to `PAID` (thu nhiều lần, cộng dồn `paidAmount`).
- [x] Overpay rejected (`amount + paidAmount <= grandTotal`); chỉ thu trên `ISSUED/PARTIAL/OVERDUE` (DRAFT/PAID/CANCELLED bị chặn).
- [x] Double-submit (same `transactionRef`) is idempotent — second call returns existing payment (unique DB constraint).
- [x] BANK_TRANSFER bắt buộc `transactionRef`; CASH bắt buộc `receivedBy` (= user hiện tại).
- [x] Refund chỉ payment `SUCCESS`: trừ `paidAmount`, tính lại `PAID/PARTIAL/ISSUED` (hoàn hết invoice PAID → `REFUNDED`).
- [x] Payment list + `GET /api/payments/summary` reconcilable per day/method for cashier close-out.
- [x] Khóa bi quan `findByIdForUpdate` chống 2 thu ngân thu cùng lúc (double-pay).
- [x] Cọc WORK/SO trước khi có Invoice: `POST /api/advances` + `/{id}/apply` cấn trừ (cùng khách, đủ nợ còn lại), hủy khi ACTIVE.

## Dependencies

Needs Invoice (07: WORK + SALES thu, PURCHASE chi). Updates supplier debt (PURCHASE unpaid) + khách nợ hợp đồng (WORK/SALES unpaid, theo `customerName/phone` snapshot — phase 2 gom thành Customer master) and revenue reports (10).
