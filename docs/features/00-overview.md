# 00 — Overview: Xưởng cơ khí nặng (vật tư kho + sửa chữa / gia công theo hợp đồng)

Domain: Xưởng cơ khí nặng, nội bộ (staff only, không có tài khoản public).
Nghiệp vụ chính: nhập vật tư vào kho → xuất ra để sửa chữa / lắp đặt thiết bị / gia công mới theo hợp đồng.
Nếu bán thì CHỈ bán vật liệu đang có trong kho, không bán sản phẩm riêng.

## Module map

| # | Feature | Spec file | Status | RBAC permissions |
|---|---------|-----------|--------|------------------|
| 1 | User management (staff only, internal) | `01-user-management.md` | Done (phase 1; StaffProfile + forgot-email deferred) | `USER_READ/WRITE`, `ROLE_MANAGE` |
| 2 | Material catalog — vật tư kho | `02-catalog-parts.md` | Done (legacy product/brand/type đã xóa) | `PRODUCT_READ/WRITE` (giữ tên quyền, hiểu là MATERIAL) |
| 3 | Supplier management | `03-supplier-management.md` | Done (phase 1) | `SUPPLIER_READ/WRITE` |
| 4 | Purchasing / Import (PO + GRN, nhập nhanh 1 bước) | `04-purchasing-import.md` | Done (phase 1: PO optional, GRN direct + quick-import chính, confirm Stock+ + auto PURCHASE invoice DRAFT) | `SUPPLIER_WRITE`, `INVENTORY_WRITE` |
| 5 | Inventory — xem tồn 1 kho nhỏ (không chuyển kho) | `05-inventory-management.md` | Done (warehouses + stocks + low-stock + transactions read API, seed kho mặc định; bỏ transfer/adjust) | `INVENTORY_READ/WRITE` |
| 6 | Work Order — sửa chữa / làm mới theo hợp đồng + vật tư tiêu hao | `11-work-order.md` | Done (phase 1: create/confirm/consume/done/cancel) | `ORDER_READ/WRITE` |
| 7 | Sales / Export — CHỈ bán vật liệu trong kho | `06-sales-export.md` | Done (phase 1: SO + GIN xuất/return, quick-sale tại quầy, auto SALES invoice DRAFT; qtyReserved hoãn) | `ORDER_READ/WRITE` |
| 8 | Invoice — WORK/SALES/PURCHASE + manual + OVERDUE job | `07-invoice-management.md` | Done (phase 1; PDF để riêng) | `INVOICE_READ/WRITE` |
| 9 | Payment management (CASH tại quầy + BANK, trả góp/ghi nợ) | `08-payment-management.md` | Done (phase 1: thu nhiều lần, refund, summary cuối ngày; advance-payment phase 2) | `PAYMENT_MANAGE` |
| 10 | Salary / HR (bậc lương + chấm công + bảng lương) | `09-salary-hr.md` | Done (phase 1: generate idempotent, approve/reject/pay, scope lương theo user; khoán WO + StaffProfile phase 2) | `PAYROLL_READ/WRITE` |
| 11 | Reports + platform (8 báo cáo + CORS + audit) | `10-reports-platform.md` | Done (phase 1; Flyway/rate-limit/secret để ops khi deploy) | read theo module |
| 12 | Customer master (mã KH + nợ theo mã + giá riêng/cọc, không hạn mức) | `12-customer.md` | Done | `CUSTOMER_READ/WRITE` |

## Current code baseline

- `module/material/*` — DONE phase 1 (`Category`, `Material`, `MaterialService/Controller`, tests).
- `module/inventory/*` — PARTIAL (`Warehouse`, `Stock`, `StockTransaction`, `InventoryService`; chưa có controller).
- `module/workorder/*` — DONE phase 1 (`WorkOrder`, `WorkOrderMaterial`, consume trừ kho, tests).
- `module/purchasing/*` — DONE phase 1 (`PurchaseOrder` DRAFT→SENT→PARTIAL→COMPLETED, `GoodsReceiptNote` DRAFT→CONFIRMED Stock+ + auto PURCHASE invoice DRAFT, tests).
- `module/sales/*` — DONE phase 1 (`SalesOrder` PENDING→CONFIRMED→DELIVERING→COMPLETED, `GoodsIssueNote` xuất/return + quick-sale, auto SALES invoice DRAFT VAT 0, tests).
- `module/payment/*` — DONE phase 1 (`Payment` CASH/BANK, thu nhiều lần + idempotent ref + khóa bi quan, refund, summary cuối ngày, tests).
- `module/payroll/*` — DONE phase 1 (`SalaryGrade`, `Attendance` + grade theo tháng, `Payroll` generate/approve/reject/pay, scope theo user, tests; khoán WO + StaffProfile phase 2).
- `module/product|brand|type/*` — ĐÃ XÓA (client chuyển sang `/api/materials`; bảng DB cũ `product/brand/type` còn lại, drop tay khi chạy Flyway).
- `module/supplier/*` — đã xong phase 1 (`code, name, paymentTerm, active, currentDebt`).
- `module/user/*`, `module/role/*`, `module/auth/*` — DONE phase 1 (RBAC + JWT + refresh/denylist + search role + guard last-admin/self + change/reset password, tests).
- `module/report/*` — DONE phase 1 (8 báo cáo read-only + tests; cache dashboard phase 2).
- Platform — DONE phase 1 (CORS localhost, audit createdBy/updatedBy; Flyway/rate-limit/secret để ops/deploy).
- `module/customer/*` — DONE (mã KH + nợ theo mã/đơn + giá riêng + cọc; backfill SQL trong `12-customer.md`; không quản lý hạn mức).
- `module/base/security/SecurityConfig.java` — JWT stateless + CORS, `GET /api/materials/**` public.
- `config/DataSeeder.java` — seeds `ADMIN` (chủ, full quyền) + staff `SALES_STAFF/WAREHOUSE_STAFF/ACCOUNTANT` (no MANAGER, no CLIENT).

## Nguyên tắc nghiệp vụ (bắt buộc)

1. Một nguồn duy nhất: `Material` vừa là hàng tồn, vừa là hàng để bán, vừa là vật tư xuất sửa/lắp. Bán hay sửa chỉ khác loại phiếu xuất.
2. Không sửa `stockQty` trực tiếp — chỉ qua `GRN/GIN/WorkOrder tiêu hao` đã confirm, mỗi lần sinh `StockTransaction` before/after.
3. `WorkOrder` sửa/làm mới theo hợp đồng phải có `vật tư tiêu hao planned vs actual` để làm căn cứ xuất `Invoice` (lao động + vật tư thực tế + VAT).
4. Bán lẻ (`Sales`) chỉ được bán vật liệu đang có trong kho (`qtyOnHand - qtyReserved >= requested`).

## Build order

1. ~~Material catalog (02)~~ — DONE (`module/material`)
2. ~~Supplier (03) + kho xem tồn (05)~~ — DONE (seed kho WH-XUONG-01; bỏ transfer/adjust theo tiệm 1 kho)
3. ~~PO + GRN nhập kho (04)~~ — DONE phase 1 (manual purchase-invoice endpoint theo GRN dồn vào bước SALES/PURCHASE invoice)
4. ~~Work Order + tiêu hao vật tư (11)~~ — DONE phase 1
5. ~~Sales vật liệu kho SO + GIN (06)~~ — DONE phase 1 (manual sales-invoice endpoint dồn vào bước hoàn thiện invoice)
6. ~~Invoice WORK/PURCHASE/SALES auto + manual + OVERDUE (07) + Payment thu/chi (08)~~ — DONE (PDF để riêng)
7. ~~Payroll (09)~~ — DONE phase 1
8. ~~Reports + platform (10)~~ — DONE phase 1 (Flyway/rate-limit/secret + PDF invoice + StaffProfile để phase 2/ops)

## Conventions used in all specs

- `BaseEntity`: `id (int, IDENTITY)`, `createdDate`, `updatedDate`.
- Money: `long` (VND, no decimals). Dates: `LocalDate` for business dates.
- Codes: `PO-2026-0001`, `GRN-2026-0001`, `GIN-2026-0001`, `INV-2026-00001`, `PAY-2026-00001`, `WO-2026-0001`.
- Stock is only mutated inside `GRN/GIN/WORK-order-consume` transactions (via `InventoryService`), never directly.
- Permission names (giữ ổn định phase 1, tránh migrate DB): `PRODUCT_READ/WRITE` = quyền Material; `ORDER_READ/WRITE` dùng chung Sales (06) + WorkOrder (11); `INVENTORY_*` cho kho/consume; `INVOICE_*` cho invoice. Phase 2 có thể tách `MATERIAL_*`, `WORKORDER_*` kèm migration `permissions` + `DataSeeder`.
- Khách hàng phase 1 = snapshot `customerName/phone` trên WO/SO/Invoice (không có bảng Customer); phase 2 gom thành Customer master khi cần công nợ theo mã KH và lịch sử hợp đồng.
