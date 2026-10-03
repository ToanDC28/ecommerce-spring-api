# 06 — Sales / Export — CHỈ bán vật liệu trong kho

## Purpose

Bán lẻ vật liệu cơ khí đang có trong kho cho khách (thợ ngoài, xưởng khác). KHÔNG bán sản phẩm riêng — cùng một `Material` với kho và WorkOrder.
Mọi SO/GIN bán đều bắt link `customer_id` (UI tạo Customer trước nếu khách chưa có SĐT trong master) — không lưu khách vãng lai tự do.
Dịch vụ sửa/làm mới theo hợp đồng thuộc WorkOrder (11), không đi qua Sales.

## Current state

DONE phase 1 — `module/sales/*` (`SalesOrder` + `issuedQty/returnedQty` theo dòng, `GoodsIssueNote` + items, `SalesOrderService` + `GoodsIssueService`, controllers `/api/sales-orders` + `/api/goods-issues` kèm `quick-sale` bán lẻ tại quầy, tests Mockito+Nest+AssertJ).

## Data model

`SalesOrder(id, code [SO-2026-0001], customer_id? (Customer master 12, optional), customerName, customerPhone, orderDate, status[PENDING,CONFIRMED,DELIVERING,COMPLETED,CANCELLED], subTotal, discount, grandTotal, note, createdBy)`
`SalesOrderItem(so_id, material_id (bắt buộc có sellPrice, active), qty, unitPrice (= Material.sellPrice snapshot lúc tạo SO), discount, lineTotal, issuedQty, returnedQty)`
`GoodsIssueNote/GIN(id, code [GIN-2026-0001], so_id? (null = bán lẻ trực tiếp), customer_name? (snapshot khi bán trực tiếp), warehouse_id, issueDate, type[EXPORT_SALE, EXPORT_RETURN], status[DRAFT,CONFIRMED,CANCELLED], createdBy)`
`GoodsIssueItem(gin_id, material_id, qty, unitPrice (snapshot SO hoặc sellPrice hiện tại nếu bán trực tiếp), lineTotal)`

## Workflow

```
PENDING (staff tạo SO, giá snapshot sellPrice, check tồn — KHÔNG giữ chỗ, xem note reservation)
 -> CONFIRMED (check tồn lại) -> DELIVERING (GIN created, xuất từng phần)
 -> COMPLETED (tất cả dòng issued đủ; GIN CONFIRMED: Stock- qua InventoryService, auto SALES invoice DRAFT VAT 0)
 CANCELLED khi chưa có GIN CONFIRMED (có rồi thì trả qua EXPORT_RETURN)
EXPORT_RETURN (khách trả): GIN type=RETURN theo SO (return <= issued - returned) -> Stock+ (refType GIN), không sinh invoice (credit note phase 2)
Bán lẻ trực tiếp: POST /api/goods-issues/quick-sale {warehouseId, customerName, items} — tạo + confirm + invoice 1 bước, không qua SO
```
Xuất cho WorkOrder (11) KHÔNG đi qua SO/GIN sales — đi qua `/work-orders/{id}/consume` + `StockTransaction WORK_ORDER`.

Note reservation (lệch spec 05, chủ ý cho tiệm nhỏ): phase 1 KHÔNG dùng `qtyReserved` — SO confirm chỉ check tồn, GIN confirm mới trừ và thiếu thì 409 chi tiết (`requested/available`). Đồng thời thấp nên check-then-deduct đủ; bổ sung giữ chỗ khi cần (ghi vào 05).

## API

```
GET  /api/sales-orders?keyword=&status=&page&size
GET  /api/sales-orders/{id}
POST /api/sales-orders {customerName, customerPhone?, discount?, note?, items:[{materialId,qty,discount?}]}  # ORDER_WRITE (PENDING, chặn trùng line + hàng nội bộ)
POST /api/sales-orders/{id}/confirm    # ORDER_WRITE (PENDING -> CONFIRMED, check tồn)
POST /api/sales-orders/{id}/cancel     # ORDER_WRITE (chặn nếu đã có GIN CONFIRMED)
GET  /api/goods-issues?keyword=&status=&salesOrderId=&warehouseId=&page&size
GET  /api/goods-issues/{id}
POST /api/goods-issues {salesOrderId?, warehouseId, customerName? (required nếu bán trực tiếp), type?, items:[{materialId,qty}]}  # INVENTORY_WRITE (DRAFT)
POST /api/goods-issues/{id}/confirm    # INVENTORY_WRITE (DRAFT -> CONFIRMED: Stock-/+, SO issued/returned/status, auto SALES invoice nếu EXPORT_SALE)
POST /api/goods-issues/{id}/cancel     # INVENTORY_WRITE (chỉ DRAFT)
POST /api/goods-issues/quick-sale {...}  # INVENTORY_WRITE (bán lẻ tại quầy 1 bước)
```

## RBAC

- `ORDER_READ`: staff.
- `ORDER_WRITE`: `SALES_STAFF/ADMIN` (app nội bộ, không có CLIENT tự đặt).
- GIN confirm: `INVENTORY_WRITE`.

## Acceptance criteria

- [x] Chỉ bán `Material isActive=true` + có `sellPrice` (hàng nội bộ bị chặn 400); trùng line bị chặn.
- [x] Thiếu hàng bị 409 chi tiết (`requested/available`) ở SO confirm và GIN confirm (check trên `stockQty`, không giữ chỗ phase 1).
- [x] Confirming GIN decrements stock, writes `StockTransaction(OUT, GIN)` before/after, triggers auto `SALES` invoice DRAFT (VAT 0).
- [x] Return (EXPORT_RETURN) tăng kho + `returnedQty`, không sinh invoice; vượt `issued - returned` bị chặn.
- [x] Không dùng Sales để xuất vật tư sửa chữa — phải qua WorkOrder (11).
- [ ] Giữ chỗ `qtyReserved` + credit note cho return — phase 2 nếu cần.

## Dependencies

Needs Material (02), Inventory (05). Produces Sales Invoice (07). Tách biệt với WorkOrder (11).
