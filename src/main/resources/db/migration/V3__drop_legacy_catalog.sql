-- V3: xóa bảng catalog legacy (module product/brand/type đã xóa code).
-- CHỈ chạy khi đã chắc không cần dữ liệu catalog cũ: backup trước
--   pg_dump -U postgres -d sport_center -t product -t brand -t type > legacy_catalog_backup.sql
-- Xóa product trước vì nó giữ FK tới brand/type; 3 bảng này không còn code nào tham chiếu.
-- Dùng IF EXISTS để chạy an toàn trên DB mới (chưa từng có bảng).

DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS brand;
DROP TABLE IF EXISTS type;
