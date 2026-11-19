package com.laptopshop.entity;

// Thành viên phụ trách: TV4
// Enum định nghĩa các trạng thái đơn hàng khớp 100% với ràng buộc CK_Orders_Status và CK_History_NewStatus trong QL_LAPTOP.sql.

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao hàng"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    RETURNED("Đã trả hàng");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
