package com.laptopshop.entity;

public enum WarrantyStatus {
    RECEIVED("Đã tiếp nhận"),
    CHECKING("Đang kiểm tra / Sửa chữa"),
    COMPLETED("Đã hoàn tất trả máy"),
    REJECTED("Từ chối bảo hành");

    private final String displayName;
    WarrantyStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
