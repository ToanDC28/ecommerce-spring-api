package com.ecommerce.sportcenter.module.payment.entity;

public enum AdvanceStatus {
    ACTIVE, // còn cọc, chưa cấn trừ
    APPLIED, // đã cấn trừ hết vào invoice
    CANCELLED // hủy cọc (hoàn tiền mặt ngoài hệ thống + ghi nhận)
}
