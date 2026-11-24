package com.laptopshop.entity;

public enum DiscountType {
    PERCENT("Phần trăm (%)"),
    FIXED("Số tiền cố định (đ)");

    private final String displayName;
    DiscountType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
