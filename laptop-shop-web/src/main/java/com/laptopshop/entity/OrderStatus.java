package com.laptopshop.entity;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PREPARING("Đang chuẩn bị hàng"),
    SHIPPING("Đang giao hàng"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    RETURNED("Đã trả hàng"),
    REFUND_REQUESTED("Yêu cầu hoàn tiền"),
    REFUNDED("Đã hoàn tiền");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Trạng thái kết thúc: không cho đổi tay nữa. */
    public boolean isClosed() {
        return this == COMPLETED || this == CANCELLED || this == RETURNED || this == REFUNDED;
    }
}
