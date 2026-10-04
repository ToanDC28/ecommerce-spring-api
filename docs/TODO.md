# TODO Backlog — tổng hợp từ các spec (phase 1 DONE, còn lại ở dưới)

> Nguồn: checkbox `- [ ]` trong `docs/features/*.md` + bảng này (quét/tick sau mỗi feature).
> Quy ước: **P2** = phase 2 (làm khi tiệm cần), **OPS** = làm khi deploy, **VERIFY** = kiểm chứng ở môi trường thật.
> Đã xong: hạn mức nợ + giá riêng + cọc trước (trước là #3) — xem `12-customer.md`, `08-payment-management.md`.

## P2 — Tính năng (ưu tiên từ trên xuống)

| # | Mục | Spec | Ghi chú |
|---|-----|------|---------|
| 1 | PDF invoice (`GET /api/invoices/{id}/pdf`: shop info, tax code, lines, VAT, contractNo/WO) | `07-invoice-management.md` | Cần thêm lib (OpenPDF) + font tiếng Việt |
| 2 | `StaffProfile` (position, hireDate, bankAccount, salaryGrade, warehouse) | `01`, `09` | Gắn `user_id` PK/FK |
| 3 | ~~Hạn mức nợ/khóa bán~~ (bỏ — chỉ xem tổng nợ + nguồn nợ), giá riêng theo khách, cọc WORK trước invoice | `12`, `08` | DONE: `CustomerMaterialPrice` + resolution, `AdvanceDeposit` + apply; nợ xem ở `GET /customers/{id}` + `/debts` |
| 4 | Khoán WO (`WorkOrderBonus`) theo `laborCost`, tránh double-count payroll | `09` | Phụ thuộc StaffProfile |
| 5 | Quên/reset mật khẩu qua email | `01` | Hiện ADMIN reset tay đủ dùng; cần mail infra |
| 6 | Credit note cho hàng trả + giữ chỗ `qtyReserved` | `06` | Tiệm nhỏ hiện chưa cần |
| ~~7~~ | ~~Tolerance % nhận thừa thép~~ | `04` | BỎ — giữ reject cứng, thừa thì PO/GRN bổ sung |
| 8 | Endpoint nợ/lịch sử mua riêng theo NCC | `03` | Tạm xem ở `reports/supplier-debt` |

## OPS — khi deploy

| # | Mục | Spec |
|---|-----|------|
| ~~9~~ | ~~Flyway/Liquibase + `ddl-auto: validate` ở prod profile~~ | `10`, `12` | DONE: deps + baseline + V2/V3; backfill Customer SQL nằm ở `12-customer.md` |
| 10 | Rate-limit `/api/auth/login` | `10` |
| 11 | Đổi `app.jwt.secret` + mật khẩu admin mặc định | `10` |
| ~~12~~ | ~~Xóa alias legacy `product/brand/type`~~ | `10` | DONE: xóa 3 module + test + public GET; bảng DB cũ drop khi Flyway |

## VERIFY — môi trường thật (cần JDK + DB/Redis)

| # | Mục | Spec |
|---|-----|------|
| 13 | Chạy `mvn test` toàn bộ (hiện chưa verify được ở máy này) | — |
| 14 | Dashboard <2s cho range 1 tháng | `10` |
| 15 | Đối soát `revenue == sum(payments SUCCESS)` theo kỳ | `10` |
| 16 | `@SpringBootTest` GRN->stock, WO-consume->stock, payment->invoice ở CI | `10` |
