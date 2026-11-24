package com.laptopshop.entity;

public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    PENDING("Đang xử lý"),
    PENDING_CONFIRMATION("Chờ xác nhận thanh toán"),
    PAID("Thanh toán thành công"),
    FAILED("Thanh toán thất bại"),
    REFUNDED("Đã hoàn tiền");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
