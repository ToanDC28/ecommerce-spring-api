-- V4: dọn cột credit_limit thừa (tính năng hạn mức nợ đã bỏ,
-- entity Customer không còn field này; ddl-auto không tự xóa cột cũ).
-- Dùng IF EXISTS để chạy an toàn trên DB mới.

ALTER TABLE IF EXISTS customer DROP COLUMN IF EXISTS credit_limit;
