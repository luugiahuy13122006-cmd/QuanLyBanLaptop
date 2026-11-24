package com.laptopshop.entity;

public enum PermissionLevel {
    NONE("Không truy cập"),
    VIEW("Chỉ xem"),
    EDIT("Xem & Thêm/Sửa"),
    FULL("Toàn quyền (gồm Xóa)");

    private final String displayName;

    PermissionLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public boolean canView() {
        return this != NONE;
    }

    public boolean canEdit() {
        return this == EDIT || this == FULL;
    }

    public boolean canDelete() {
        return this == FULL;
    }
}
