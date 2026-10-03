# 05 — Inventory Management (kho vật tư cơ khí)

## Purpose

Track on-hand quantity per warehouse cho vật tư cơ khí nặng, every movement auditable. Một nguồn tồn duy nhất cho cả bán lẻ (06) và WorkOrder sửa/làm mới (11).

## Current state

DONE (tiệm 1 kho nhỏ, chỉ xem) — `WarehouseService` (list/get/create) + `InventoryService` đọc (`searchStocks`, `lowStock`, `searchTransactions`) + controllers `GET /api/warehouses`, `GET /api/stocks`, `/low-stock`, `/transactions`, seed kho mặc định `WH-XUONG-01`, tests. Ghi kho (increase/decrease + `StockTransaction`) đã xong từ trước qua GRN/GIN/WO-consume.

Bỏ theo quyết định tiệm 1 kho: `POST /stocks/transfer` (không chuyển kho), `POST /stocks/adjust` (hao hụt — khi nào cần kiểm kê sẽ làm riêng).

## Data model

`Warehouse(id, code UNIQUE [WH-XUONG-01], name, address, manager_id?, isActive)` — xưởng thường 1 kho chính + bãi thép riêng.
`Stock(id, warehouse_id, material_id (thay part_id), qtyOnHand, qtyReserved, UNIQUE(warehouse_id, material_id))`
`StockTransaction(id, material_id, warehouse_id, type[IN,OUT], refType[GRN,GIN,WORK_ORDER,ADJUST,TRANSFER], refId (code PO/GRN/GIN/WO), qtyBefore, qtyChange, qtyAfter, createdBy, createdDate)`
`StockAdjust(id, code, warehouse_id, material_id, qtyBefore, qtyAfter, reason[DAMAGED,LOST,FOUND,CORRECTION,H hao hụt cắt thép], status, approvedBy)`

## Rules

- Never update `Stock` directly — only via confirmed GRN (04), GIN sale (06), WorkOrder consume (11).
- Tiệm 1 kho nhỏ: không giữ chỗ `qtyReserved`, không chuyển kho; SO confirm chỉ check tồn, GIN confirm mới trừ (thiếu → 409).
- `qtyOnHand >= 0` always (reject oversell / over-consume).
- Hao hụt cơ khí (cắt thép, dầu rơi vãi): chưa có phiếu adjust — khi nào cần kiểm kê sẽ làm `POST /stocks/adjust` riêng.

## API

```
GET  /api/warehouses                                          # INVENTORY_READ (tiệm nhỏ thường 1 kho, seed sẵn WH-XUONG-01)
GET  /api/warehouses/{id}                                     # INVENTORY_READ
POST /api/warehouses {code, name, address?}                   # INVENTORY_WRITE
GET  /api/stocks?warehouseId=&materialId=&lowStockOnly=&page&size   # INVENTORY_READ
GET  /api/stocks/low-stock?warehouseId=                       # INVENTORY_READ
GET  /api/stocks/transactions?materialId=&warehouseId=&refType=&page&size  # refType GRN/GIN/WORK_ORDER
```

## RBAC

- `INVENTORY_READ`: all staff.
- `INVENTORY_WRITE`: `WAREHOUSE_STAFF, ADMIN` (+ `SALES_STAFF` kiêm kho).

## Acceptance criteria

- [x] Every GRN/GIN/WO-consume creates a `StockTransaction` with before/after (qua `InventoryService`).
- [x] `GET /api/stocks` xem tồn theo kho/vật tư + `lowStockOnly`.
- [x] `GET /api/stocks/low-stock` returns `qtyOnHand <= material.minStock`.
- [x] `GET /api/stocks/transactions` lịch sử nhập/xuất theo vật tư/kho/loại phiếu.
- [x] Seed kho mặc định `WH-XUONG-01` để nhập/xuất chạy ngay.
- Bỏ (tiệm 1 kho): transfer + adjust hao hụt — làm riêng khi cần kiểm kê.

## Dependencies

Needs Material (02). Used by Purchasing (04), WorkOrder (11) và Sales (06).
