# 07 — Invoice Management (WORK + SALES + PURCHASE, có VAT)

## Purpose

Single source of truth cho công nợ xưởng cơ khí:
- `WORK`: thu từ WorkOrder sửa/làm mới (11) — lao động + vật tư tiêu hao thực tế.
- `SALES`: thu từ bán vật liệu kho (06).
- `PURCHASE`: trả cho NCC từ GRN (04).

## Current state

DONE phase 1 (PDF để riêng) — `module/invoice/*`: WORK auto + PURCHASE auto + SALES auto, manual `POST /api/goods-issues|goods-receipts/{id}/invoice` (guard trùng refCode), `issue/cancel`, OVERDUE job hằng đêm 01:00, tests.

## Data model

`Invoice(id, code UNIQUE [INV-2026-00001], type[WORK,SALES,PURCHASE], work_order_id?/work_order_code? (WORK), so_id?/so_code? (SALES), supplier_id?/supplier_name? (PURCHASE, snapshot), ref_code? (GIN/GRN code gốc), customer_id (required với WORK/SALES) + customerName/Phone snapshot tự lấy theo link, issueDate, dueDate, subTotal, discountAmount, vatRate[0,8,10], vatAmount, grandTotal, paidAmount, status[DRAFT,ISSUED,PARTIAL,PAID,OVERDUE,CANCELLED,REFUNDED], createdBy)`
`InvoiceItem(invoice_id, material_id? (null cho dòng labor/phụ phí/auto PURCHASE/SALES — description là snapshot), description, qty, unitPrice, discount, lineTotal)`
- Dòng WORK: auto-gen từ `WorkOrderMaterial.qtyActual * unitSellPrice` + 1 dòng `laborCost` + `overheadCost` từ WorkOrder DONE (VAT mặc định 10).
- Dòng SALES: auto-gen khi GIN confirm từ issue lines (giá bán snapshot, VAT mặc định 0 bán lẻ).
- Dòng PURCHASE: auto-gen khi GRN confirm từ receipt lines (VAT mặc định 10).

Formulas: `subTotal = sum(lineTotal)`, `vatAmount = (subTotal - discount) * vatRate`, `grandTotal = subTotal - discount + vatAmount`.

## Lifecycle

```
DRAFT (editable) -> ISSUED (code assigned, immutable lines, stock đã trừ qua GRN/GIN/WO-consume)
 -> PARTIAL (0 < paid < grand) -> PAID (paid == grand)
 ISSUED/PARTIAL + dueDate < today -> OVERDUE (nightly job)
 ISSUED -> CANCELLED (before payment; WorkOrder về lại DONE) | PAID -> REFUNDED (credit note)
WORK invoice chỉ xuất khi WorkOrder DONE (đã chốt qtyActual + labor).
```

## API

```
POST /api/invoices/work {workOrderId, vatRate, discount}              # INVOICE_WRITE (từ actual + labor) — DONE
POST /api/goods-issues/{id}/invoice                                 # INVOICE_WRITE (SALES tay theo GIN CONFIRMED, guard trùng) — DONE
POST /api/goods-receipts/{id}/invoice                               # INVOICE_WRITE (PURCHASE tay theo GRN CONFIRMED, guard trùng) — DONE
GET  /api/invoices?type=&status=&keyword=                             # keyword khớp code/customer/supplier/WO/SO — DONE
GET  /api/invoices/{id}  (kèm lines + link workOrder/supplier)        # DONE
POST /api/invoices/{id}/issue | /api/invoices/{id}/cancel             # INVOICE_WRITE — DONE (cancel mở lại WO DONE)
GET  /api/invoices/{id}/pdf                                           # pending (cần thêm lib PDF + font tiếng Việt)
GET  /api/invoices/overdue                                            # DONE truy vấn theo status; job OVERDUE hằng đêm 01:00 — DONE
```

## RBAC

- `INVOICE_READ`: `SALES_STAFF (sales/work only), ACCOUNTANT, ADMIN`.
- `INVOICE_WRITE`: `ACCOUNTANT, ADMIN`.

## Acceptance criteria

- [x] Code unique, sequential per year; totals recomputed server-side (never trust client math).
- [x] WORK invoice lines khớp `WorkOrderMaterial.qtyActual` + `laborCost/overhead` tại lúc DONE (snapshot, sau đổi giá không ảnh hưởng).
- [x] PURCHASE invoice auto DRAFT khi GRN confirm (lines từ receipt + VAT 10) + xuất tay guard trùng refCode.
- [x] SALES invoice auto DRAFT khi GIN confirm (lines từ issue + VAT 0 bán lẻ) + xuất tay guard trùng refCode.
- [x] `OVERDUE` job runs daily 01:00 (`markOverdue`); `GET /overdue` = truy vấn theo status — DONE.
- [ ] PDF — pending (cần thêm lib + font tiếng Việt, làm riêng).
- [ ] PDF contains shop info, tax code, line items (vật tư + nhân công), VAT breakdown, contractNo/WO code.
- [x] SALES invoice + manual purchase endpoint — DONE (`POST /api/goods-issues|goods-receipts/{id}/invoice`, guard trùng).

## Dependencies

Consumes WorkOrder DONE (11) / GIN (06) / GRN (04). Paid via Payment (08). Supplier debt + khách nợ hợp đồng derived from here.
