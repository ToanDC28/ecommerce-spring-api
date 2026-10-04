# 10 — Reports + Platform (Dashboard, Audit, Ops)

## Purpose

Owner/manager visibility + production hardening.

## Reports — DONE phase 1 (`module/report/*`: `ReportService` read-only + `ReportController /api/reports/*`, tests `ReportServiceImplTest`)

```
GET /api/reports/revenue?from=&to=&groupBy=day|month      # sum WORK + SALES invoices PAID/PARTIAL (PURCHASE loại ra)
GET /api/reports/revenue-by-type?from=&to=                # tách WORK vs SALES
GET /api/reports/top-materials?limit=10                   # vật tư tiêu hao/bán chạy (theo qtyActual + sales qty)
GET /api/reports/workorder-profit?from=&to=               # theo WO: agreedPrice - (actual*unitCost + labor + overhead)
GET /api/reports/stock-value                               # sum(stockQty * costPrice) + cảnh báo low-stock
GET /api/reports/supplier-debt | /api/reports/customer-debt  # NCC (PURCHASE unpaid) | khách (WORK/SALES unpaid, ưu tiên customerId, fallback snapshot tên/SĐT)
GET /api/reports/salary-cost?period=
GET /api/reports/profit?from=&to=                          # (WORK+SALES revenue) - (material actual cost) - salary
```

- COGS = `WorkOrderMaterial.qtyActual * unitCost` (snapshot lúc xuất) + `SO qty * costPrice HIỆN TẠI` (xấp xỉ — snapshot cost theo thời điểm bán để phase 2).
- Lương trong profit = payroll `PAID` có kỳ giao với range (so sánh chuỗi YYYY-MM).
- Khách hàng có mã thì group theo `customerId` (12), khách vãng lai group theo snapshot tên/SĐT.
- Cache heavy reports (Redis đã có trong `docker/docker-compose.yml`) — phase 2 khi dashboard chậm.

## Platform hardening

- [x] Pagination/sort chuẩn style (`PageResponse` + `Search*Request.toSort()`; reports là read-model gom sẵn nên trả list trực tiếp, không phân trang).
- [x] Audit: `createdBy/updatedBy` trên `BaseEntity` (`@CreatedBy/@LastModifiedBy` + `AuditingConfig.AuditorAware` lấy username, `system` khi seeder/CLI) — cột auto-add qua `ddl-auto: update`.
- [x] CORS cho frontend nội bộ (`SecurityConfig.corsConfigurationSource`: `localhost:*`, GET/POST/PUT/PATCH/DELETE/OPTIONS, credentials).
- [x] Document `server.port=9000`.
- [x] Tests: service unit tests pattern Mockito+Nest+AssertJ (có `ReportServiceImplTest` cho revenue/stock/debt).
- [x] Xóa alias legacy `product/brand/type` (module + test + SecurityConfig; bảng DB cũ drop tay khi chạy Flyway).
- [x] Flyway wired (`flyway-core` + `flyway-database-postgresql`, `baseline-on-migrate`, locations `db/migration`); V2 dọn `stored_name`, V3 xóa bảng legacy product/brand/type (backup trước), V4 dọn `credit_limit` (bỏ hạn mức).
- Quy tắc từ nay (bắt buộc): **sửa entity = thêm migration `V{n}__...sql` cùng PR**. `ddl-auto: update` ở dev tự thêm cột mới nên dev chạy được ngay, nhưng nó KHÔNG xóa cột cũ / nới NOT NULL / drop bảng — thiếu migration là prod `validate` fail hoặc rác schema như vụ `stored_name`. Dev giữ `ddl-auto: update`; prod (`SPRING_PROFILES_ACTIVE=prod`) dùng `validate`.
- [ ] Rate-limit `/api/auth/login` (brute-force; cân nhắc Bucket4j khi deploy — app nội bộ ưu tiên thấp).
- [ ] Đổi `app.jwt.secret` + mật khẩu admin mặc định khi deploy.
- [ ] `@SpringBootTest` cho GRN->stock, WO-consume->stock và payment->invoice transitions (cần DB/Redis thật, chạy ở CI).

## RBAC

Reports require the matching read permission (`INVOICE_READ` cho revenue/profit/debt, `INVENTORY_READ` cho stock/top-materials, `PAYROLL_READ` cho salary, `ORDER_READ` cho workorder-profit).

## Acceptance criteria

- [x] Dashboard đủ 8 báo cáo theo spec (revenue, revenue-by-type, top-materials, stock-value, supplier/customer-debt, salary-cost, profit, workorder-profit).
- [ ] Dashboard loads < 2s for 1 month range (đo ở deploy thật).
- [ ] All money fields reconcile: `revenue == sum(payments SUCCESS)` for period (kiểm tra ở deploy).
- [x] `ddl-auto: validate` ở prod profile (Flyway migrate trước JPA theo thứ tự Boot). DB mới: chạy 1 lần profile mặc định để dựng schema rồi mới bật prod.
