-- V2: dọn cột thừa do ddl-auto để lại (vụ stored_name vỡ insert upload ảnh).
-- ddl-auto:update thêm cột mới được nhưng không bao giờ xóa cột cũ / nới constraint.
-- Dùng IF EXISTS để chạy an toàn trên cả DB mới (chưa từng có cột này).

ALTER TABLE IF EXISTS work_order_attachment DROP COLUMN IF EXISTS stored_name;
