package com.laptopshop.entity;

public enum SerialStatus {
    IN_STOCK("Trong kho"),
    SOLD("Đã bán / Đang bảo hành"),
    WARRANTY_CLAIM("Đang xử lý bảo hành"),
    VOID("Đã thu hồi / Hủy");

    private final String displayName;
    SerialStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
