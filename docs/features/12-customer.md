# 12 — Customer Master (mã KH, công nợ theo mã)

## Purpose

Mọi invoice bán/sửa đều link mã KH `KH-001` (UI tạo khách trước nếu chưa có SĐT). Không lưu khách vãng lai tự do trong invoice — chưa có SĐT thì lập mã mới (tên + SĐT bắt buộc).

## Current state

DONE — `module/customer/*`: CRUD + search + debts (tổng nợ + nguồn nợ theo từng invoice WO/SO) + work-orders + `CustomerMaterialPrice` (giá riêng) + tests. WO bắt buộc link, SO/Invoice link optional.
Không quản lý hạn mức (quyết định tiệm nhỏ): xem tổng nợ ở `GET /{id}` (`totalOwed` + `openInvoiceCount`) và nguồn nợ ở `/{id}/debts` là đủ.

## Data model

`Customer(id, code UNIQUE [KH-001] auto, name, phone (chuẩn hóa 0xxxxxxxxx, unique khi có), address, type[LE_QUEN, HOP_DONG], isActive)`
- `WorkOrder.customer_id` (required) + snapshot `customerName/Phone` hiển thị.
- `SalesOrder.customer_id` (required) + snapshot hiển thị.
- `Invoice.customer_id` (required với WORK/SALES, auto theo WO/SO/GIN) + snapshot hiển thị.
- Báo cáo nợ group theo mã KH (invoice cũ trước quy tắc này còn null → fallback snapshot).

## Rules

- SĐT chuẩn hóa khi ghi (`+84→0`, bỏ khoảng trắng, phải `0\d{9}`); trùng SĐT với mã khác → 400 kèm mã KH cũ.
- WO bắt buộc `customerId` (khách inactive → 400); SO/invoice trực tiếp cho phép null.
- Đổi tên/SĐT Customer không sửa snapshot trên WO/SO/Invoice cũ (giữ lịch sử đúng thời điểm).
- Xóa cứng không có — chỉ `setActive(false)` (soft-delete `@SQLDelete`).

## API

```
GET  /api/customers?keyword=&type=&active=&page&size   # CUSTOMER_READ (autocomplete khi tạo đơn)
GET  /api/customers/{id}                                # CUSTOMER_READ (kèm openInvoiceCount + totalOwed)
POST /api/customers {name, phone?, address?, type?}     # CUSTOMER_WRITE (auto KH-001)
PUT  /api/customers/{id}                                # CUSTOMER_WRITE
PATCH /api/customers/{id}/active                        # CUSTOMER_WRITE
GET  /api/customers/{id}/debts                          # CUSTOMER_READ (invoice WORK/SALES chưa trả)
GET  /api/customers/{id}/work-orders                    # CUSTOMER_READ (lịch sử hợp đồng)
```

## RBAC

- `CUSTOMER_READ`: `ADMIN, SALES_STAFF, ACCOUNTANT`.
- `CUSTOMER_WRITE`: `ADMIN, SALES_STAFF` (bán hàng tự lập mã KH mới).

## Migration backfill (chạy 1 lần khi deploy, chưa có Flyway)

```sql
-- 1. Gom khách distinct từ WO/SO (chuẩn hóa SĐT thủ công trước khi chạy)
INSERT INTO customer (code, name, phone, type, is_active)
SELECT 'KH-TMP-' || row_number() OVER (),
       customer_name, customer_phone, 'HOP_DONG', true
FROM (SELECT DISTINCT customer_name, customer_phone FROM work_order
      UNION
      SELECT DISTINCT customer_name, customer_phone FROM sales_order) s;
-- 2. Gộp trùng tay (2 row cùng SĐT/sai chính tả) rồi đánh lại code KH-001...
-- 3. Link ngược:
UPDATE work_order wo SET customer_id = (SELECT id FROM customer c
  WHERE c.name = wo.customer_name AND COALESCE(c.phone,'') = COALESCE(wo.customer_phone,'') LIMIT 1);
UPDATE sales_order so SET customer_id = (SELECT id FROM customer c
  WHERE c.name = so.customer_name AND COALESCE(c.phone,'') = COALESCE(so.customer_phone,'') LIMIT 1);
UPDATE invoice i SET customer_id = ...; -- tương tự theo work_order_id/so_id
```

## Acceptance criteria

- [x] Tạo KH auto mã, trùng SĐT → 400 kèm mã cũ; sai format SĐT → 400.
- [x] WO/SO/GIN/invoice thiếu link KH → 400; khách inactive → 400.
- [x] `GET /{id}` kèm nợ mở; `/debts` chỉ invoice chưa trả; `/work-orders` lịch sử.
- [x] Báo cáo `customer-debt` theo mã KH (fallback snapshot cho dữ liệu cũ).
- [x] Giá riêng theo khách (`/{id}/prices`, resolution giá riêng → chung → null).
- [x] Cọc trước xem ở 08 (`AdvanceDeposit` + apply).
- Bỏ (không quản lý hạn mức): `creditLimit` + chặn vượt nợ + khóa bán khi quá hạn.
