# 11 — Work Order: sửa chữa / làm mới theo hợp đồng + vật tư tiêu hao

## Purpose

Nghiệp vụ cốt lõi của xưởng cơ khí nặng. Khách đặt sửa chữa đồ cơ khí nặng hoặc đặt làm mới thiết bị theo hợp đồng.
Mỗi WorkOrder phải ghi nhận vật tư tiêu hao (planned vs actual) để làm căn cứ trừ kho và xuất Invoice.

## Current state

TODO — chưa có code. Ưu tiên làm trước Sales (06) và Invoice (07).

## Data model

`WorkOrder(id, code UNIQUE [WO-2026-0001], type[REPAIR, MANUFACTURE_NEW], contractNo?, customer_id (required, Customer master 12), customerName/Phone snapshot hiển thị, machineInfo (tên máy/model/tình trạng nhận), receivedDate, dueDate, status[DRAFT, CONFIRMED, IN_PROGRESS, DONE, INVOICED, CANCELLED], laborCost, overheadCost, agreedPrice?, note, createdBy)`
`WorkOrderMaterial(id, work_order_id, material_id, qtyPlanned (dự toán báo giá), qtyActual (thực xuất, default = 0), unitCost (giá vốn tại thời điểm xuất, snapshot), unitSellPrice (đơn giá tính cho khách, snapshot), note)`
- `qtyPlanned` dùng để báo giá / duyệt hợp đồng.
- `qtyActual` dùng để trừ kho + tính invoice. Chênh lệch planned vs actual = lãi/lỗ vật tư.
- `unitCost/unitSellPrice` snapshot tại lúc confirm để sau này giá Material đổi không làm sai lịch sử.

`WorkOrderStatusHistory(work_order_id, fromStatus, toStatus, changedBy, changedAt, note)` — optional phase 2.

## Workflow

```
DRAFT (thợ/kinh doanh tạo, nhập machineInfo + qtyPlanned)
 -> CONFIRMED (chủ/ADMIN duyệt, chốt phạm vi làm)
 -> IN_PROGRESS (thợ bắt đầu, xuất vật tư dần → qtyActual+, Stock- qua StockTransaction REPAIR_OUT/MANUFACTURE_OUT)
 -> DONE (nghiệm thu, chốt qtyActual, chốt laborCost/overhead)
 -> INVOICED (ACCOUNTANT xuất Invoice WORK từ actual + labor, xem 07)
CANCELLED trước IN_PROGRESS thì hoàn lại reservation (nếu có), không trừ kho.
```

Rules:
- Chỉ xuất `Material isActive=true`.
- Xuất vượt `qtyOnHand` thì reject 409 kèm shortfall (thừa thì mua gấp bằng GRN trực tiếp).
- `CONFIRMED/IN_PROGRESS` mới được xuất vật tư; `DONE` khóa `qtyActual` (muốn sửa phải reopen về IN_PROGRESS).
- Mọi lần xuất đều trong `@Transactional`: `WorkOrderMaterial.qtyActual+` + `Stock.qtyOnHand-` + `StockTransaction(OUT, refType=WORK_ORDER, refId=WO code)`.
- `INVOICED` xong thì WorkOrder immutable (muốn sửa phải hủy Invoice).

## API

```
POST /api/work-orders {type, customerName, machineInfo, dueDate, items:[{materialId, qtyPlanned}], laborCost}  # ORDER_WRITE
GET  /api/work-orders?type=&status=&keyword=&from=&to=&page&size   # ORDER_READ
GET  /api/work-orders/{id}                                         # ORDER_READ (kèm items + stock availability)
PUT  /api/work-orders/{id}                                         # ORDER_WRITE (chỉ DRAFT/CONFIRMED)
POST /api/work-orders/{id}/confirm                                 # ORDER_WRITE (chủ/ADMIN duyệt)
POST /api/work-orders/{id}/consume {items:[{materialId, qty}]}     # INVENTORY_WRITE (xuất thực tế → qtyActual+, Stock-)
POST /api/work-orders/{id}/done                                    # ORDER_WRITE (chốt actual + labor)
POST /api/work-orders/{id}/cancel                                  # ORDER_WRITE
GET  /api/work-orders/{id}/materials                               # ORDER_READ (planned vs actual, shortfall)
```

## RBAC

- `ORDER_READ`: all staff.
- `ORDER_WRITE`: `SALES_STAFF/ADMIN` (tạo/duyệt/done/cancel) + thợ tạo DRAFT và consume (mở thêm cho `WAREHOUSE_STAFF` ở endpoint `/consume`).
- `/consume`: `INVENTORY_WRITE` (`WAREHOUSE_STAFF, SALES_STAFF kiêm kho, ADMIN`).

## Acceptance criteria

- [x] Tạo WO với `qtyPlanned`; confirm chuyển `DRAFT→CONFIRMED`.
- [x] `/consume` trừ `Stock.qtyOnHand`, cộng `qtyActual`, sinh `StockTransaction(OUT, WORK_ORDER)` với before/after.
- [x] Xuất quá tồn bị 409 + chi tiết thiếu (`materialId, requested, available`).
- [x] `DONE` chốt actual + labor; `INVOICED` khóa WO (sửa phải hủy invoice).
- [x] Báo cáo lãi/lỗ theo WO: `agreedPrice - (sum(qtyActual*unitCost) + laborCost + overhead)` (`GET /api/reports/workorder-profit`).

## Dependencies

Cần Material (02), Stock (05). Sinh GIN logic nội bộ (không qua Sales 06) + Invoice WORK (07).
Khách hàng link `customer_id` bắt buộc (UI tạo Customer trước nếu chưa có SĐT); snapshot `customerName/Phone` hiển thị tự lấy theo link.
