# 04 — Purchasing / Import (PO + GRN) — nhập vật tư kho xưởng cơ khí

## Purpose

Mua vật tư cơ khí từ suppliers và nhập vào kho (goods in). Nguồn tăng tồn duy nhất (cùng với return từ WorkOrder/Sales).

Tiệm nhỏ lẻ: thực tế hàng đã mua rồi, phiếu chỉ ghi lại lịch sử nhập. Flow chính là **nhập nhanh 1 bước** (GRN trực tiếp, không qua PO); PO chỉ dùng cho trường hợp hiếm (đặt trước, hàng về sau).

## Current state

DONE phase 1 — `module/purchasing/*` (`PurchaseOrder` + `PurchaseOrderItem.receivedQty`, `GoodsReceiptNote` + `GoodsReceiptItem`, `PurchaseOrderService` + `GoodsReceiptService`, controllers `/api/purchase-orders` + `/api/goods-receipts`, tests Mockito+Nest+AssertJ). Supplier (03) đã xong phase 1.

## Data model

`PurchaseOrder(id, code UNIQUE [PO-2026-0001], supplier_id, orderDate, expectedDate, status[DRAFT,SENT,PARTIAL,COMPLETED,CANCELLED], totalAmount, note, createdBy)`
`PurchaseOrderItem(po_id, material_id (thay part_id), qty, unitCost, lineTotal, receivedQty)`
`GoodsReceiptNote/GRN(id, code [GRN-2026-0001], po_id? (null = mua trực tiếp), warehouse_id (required), supplier_id (required khi không có PO), receiptDate, type[IMPORT_PURCHASE, IMPORT_RETURN_WORK, IMPORT_RETURN_SALE], status[DRAFT,CONFIRMED,CANCELLED], createdBy)`
`GoodsReceiptItem(grn_id, material_id, qty, unitCost, batchNo?, lineTotal)`
- `IMPORT_PURCHASE`: nhập mua mới từ NCC.
- `IMPORT_RETURN_WORK/SALE`: vật tư thừa từ WorkOrder (11) / khách trả lại từ Sales (06) nhập lại kho.

## Workflow

```
DRAFT PO -> SENT (to supplier) -> PARTIAL (1st GRN confirmed)
  -> COMPLETED (all qty received) | CANCELLED
GRN DRAFT -> CONFIRMED: Stock+ (05), update PO received qty/status (nếu có PO), generate PURCHASE invoice (07)

Flow chính tiệm nhỏ (không PO):
POST /api/goods-receipts/quick-import {warehouseId, supplierId, items} — tạo + confirm 1 bước atomic
(Stock+, auto PURCHASE invoice DRAFT). PO là optional, chỉ khi đặt trước.
```

Rules: `CONFIRMED` GRN is immutable (revert via new return GRN). All in one `@Transactional` with row lock on `Stock`. Vật tư cơ khí nặng thường theo `KG/MET/BO`, kiểm tra quy cách + số lượng thực cân/đo khi confirm.

## API

```
GET  /api/purchase-orders?keyword=&supplierId=&status=&page&size
GET  /api/purchase-orders/{id}
POST /api/purchase-orders {supplierId, expectedDate?, note?, items:[{materialId,qty,unitCost}]}  # SUPPLIER_WRITE (DRAFT, 1 line/material, NCC phải active)
POST /api/purchase-orders/{id}/send      # SUPPLIER_WRITE (DRAFT -> SENT)
POST /api/purchase-orders/{id}/cancel    # SUPPLIER_WRITE (DRAFT/SENT, chặn nếu đã có GRN CONFIRMED)
GET  /api/goods-receipts?keyword=&status=&purchaseOrderId=&warehouseId=&page&size
GET  /api/goods-receipts/{id}
POST /api/goods-receipts {purchaseOrderId?, warehouseId, supplierId? (required nếu không có PO), type?, items:[{materialId,qty,unitCost,batchNo?}]}  # INVENTORY_WRITE (DRAFT)
POST /api/goods-receipts/{id}/confirm    # INVENTORY_WRITE (DRAFT -> CONFIRMED: Stock+, PO received/status nếu có PO, auto PURCHASE invoice DRAFT)
POST /api/goods-receipts/{id}/cancel     # INVENTORY_WRITE (chỉ DRAFT)
POST /api/goods-receipts/quick-import {warehouseId, supplierId?, purchaseOrderId?, type?, items}  # INVENTORY_WRITE (flow chính tiệm nhỏ: tạo + confirm 1 bước)
```

## RBAC

- Create/send PO: `SUPPLIER_WRITE`.
- Create/confirm GRN: `INVENTORY_WRITE` (`WAREHOUSE_STAFF`, `ADMIN`; `SALES_STAFF` kiêm kho cũng có quyền này).

## Acceptance criteria

- [x] Confirming GRN increases `Stock.qtyOnHand` (+ `Material.stockQty`) and writes `StockTransaction(IN, refType=GRN)` with before/after.
- [x] Over-receipt beyond PO qty rejected with detail (`ordered / already received / this GRN`); material ngoài PO bị chặn.
- [x] Cancelling a DRAFT PO/GRN leaves stock untouched (CONFIRMED GRN chỉ revert qua return GRN; SENT PO có GRN CONFIRMED không được cancel).
- [x] GRN confirm auto sinh PURCHASE invoice DRAFT (kế toán issue sau ở 07).
- Bỏ (quyết định tiệm nhỏ): tolerance % nhận thừa — giữ reject cứng kèm chi tiết, thừa thì tạo PO/GRN bổ sung.

## Dependencies

Needs Supplier (03), Material (02), Warehouse/Stock (05). Produces Purchase Invoice (07). Vật tư thừa từ WorkOrder (11) có thể nhập lại qua GRN `IMPORT_RETURN_WORK`.
