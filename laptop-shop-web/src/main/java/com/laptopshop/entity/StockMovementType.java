package com.laptopshop.entity;

public enum StockMovementType {
    IMPORT("Nhập kho"),
    SALE("Xuất bán"),
    CANCEL_RESTORE("Hoàn kho do hủy đơn"),
    RETURN_RESTORE("Hoàn kho do trả hàng"),
    MANUAL_ADJUST("Điều chỉnh thủ công"),
    SOCKET_ADJUST("Điều chỉnh qua Socket");

    private final String displayName;

    StockMovementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
