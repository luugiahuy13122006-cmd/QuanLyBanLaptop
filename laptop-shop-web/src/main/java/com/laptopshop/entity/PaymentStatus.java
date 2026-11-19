package com.laptopshop.entity;

// Thành viên phụ trách: TV4
// Enum định nghĩa trạng thái thanh toán khớp 100% với ràng buộc CK_Payments_Status trong QL_LAPTOP.sql.

public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    PENDING("Đang xử lý"),
    PAID("Đã thanh toán"),
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
