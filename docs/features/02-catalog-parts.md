# 02 — Material catalog: vật tư kho (thay Product e-commerce)

## Purpose

Một nguồn duy nhất cho xưởng cơ khí nặng: `Material` vừa là hàng tồn kho, vừa là hàng để bán lẻ, vừa là vật tư xuất cho sửa chữa / lắp đặt / gia công mới.
Bỏ mô hình `Product` bán lẻ (`name, description, price, imageUrl, brand, type`).

## Current state — PARTIAL (cần refactor)

- `module/product/entity/Product.java`: `name, description, price, imageUrl, brand, type`.
- `module/brand/entity/Brand.java`: `name + products`.
- `module/type/entity/Type.java`: `name + products` (tên chung chung, không phản ánh cơ khí).
- `module/product/controller/ProductController.java`: `GET /api/product?keyword&brandId&typeId...`, `POST /api/product` (`PRODUCT_WRITE`).
- `module/product/service/impl/ProductServiceImpl.java`: pagination + `Specification`.

## TODO — data model (Material)

`Category(id, code UNIQUE [CAT-THEP-TAM], name, parent_id?, isActive)` — e.g. Thép tấm > Thép CT3; Bu-lông > Bu-lông M20; Dầu mỡ > Dầu thủy lực; Vòng bi > SKF.
`Material(id, sku UNIQUE [VT-THEP-CT3-10MM], name, category_id, brand?, unit, costPrice, sellPrice?, stockQty (read-only), minStock, location?, imageUrl?, isActive,
  materialGrade? (mác: CT3/C45/Inox 304), standard? (JIS/ASTM/TCVN/DIN), spec? (quy cách chính: M12x50, 10x1500x6000),
  thicknessMm?/widthMm?/lengthMm?/diameterMm? (số thực, mm), strengthGrade? (4.8/8.8/10.9/12.9),
  detail? (TEXT: bước ren, lớp mạ, xử lý nhiệt...))`
- Tất cả 9 trường spec đều optional (tạo không cần nhập); update chỉ set field non-null.
- Lọc: `keyword` khớp cả `spec`; `materialGrade` lọc chính xác theo mác.
- `sku` unique; `(category, name, spec)` nên unique logic để tránh trùng quy cách.
- `unit` hiện tại: `CAI, KG, MET, LIT, BO, HOP, CUON` (`MaterialUnit`); phát sinh thép tấm/thanh thì bổ sung `TAM, THANH, M2` thay vì quy đổi thủ công.
- `sellPrice` chỉ dùng khi bán lẻ (06); vật tư chỉ dùng nội bộ có thể để null.
- `stockQty` KHÔNG sửa trực tiếp — chỉ qua GRN/GIN/WorkOrder (04/05/11).

Migration: `Type` -> `Category` (+ `parent, code`), `Product` -> `Material` (+ `sku, unit, costPrice/sellPrice, stockQty, minStock, location`), `Brand` giữ optional hoặc gom vào `Material.brand` text.
Legacy `GET /api/product/**`, `/api/brands/**`, `/api/types/**` ĐÃ XÓA (module + test + public GET trong `SecurityConfig`); bảng DB cũ drop tay khi chạy Flyway.

## API to add/change

```
GET  /api/materials?keyword=&sku=&categoryId=&brand=&lowStockOnly=&isActive=&page&size
GET  /api/materials/{id}
GET  /api/materials/{id}/stock
POST /api/materials                       # PRODUCT_WRITE (= MATERIAL_WRITE)
PUT  /api/materials/{id}                   # PRODUCT_WRITE
PATCH /api/materials/{id}/active           # PRODUCT_WRITE
GET  /api/categories (Category trong module/material — brand chỉ còn text trên Material)
```

Giữ `GET /api/materials` public (tra cứu kho); writes cần `PRODUCT_WRITE` (`ADMIN` — chủ nhập danh mục; staff bán chỉ cần `PRODUCT_READ`) — giữ tên quyền cũ để khỏi migrate DB.

## Acceptance criteria

- [x] `sku` unique; duplicate trả 400 (`BusinessValidationException`).
- [x] `unit, costPrice >= 0, sellPrice >= 0`, `minStock` dùng cho low-stock alert (05/reports).
- [x] `stockQty` chỉ đổi qua transaction; API create/update không nhận `stockQty`.
- [x] Soft-disable (`isActive=false`) ẩn khỏi bán/sửa/nhập mới nhưng giữ lịch sử.
- [x] Bán (06) và WorkOrder (11) chỉ chọn `Material isActive=true`; đủ tồn check ở confirm/consume (409 chi tiết).

## Dependencies

Cần trước Purchasing (04), Inventory (05), WorkOrder (11), Sales (06), Invoice (07). Làm module này đầu tiên.
