package com.ecommerce.sportcenter.module.payroll.entity;

public enum PayrollStatus {
    PENDING, // (giữ tương thích dữ liệu cũ) xem như READY_TO_PAY
    READY_TO_PAY, // worker sinh xong, chờ admin review
    APPROVED,
    PAID,
    REJECTED
}
