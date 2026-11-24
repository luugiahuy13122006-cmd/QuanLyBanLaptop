package com.laptopshop.entity;

public enum AdminModule {
    STATISTICS("Thống kê & Báo cáo", "bi-bar-chart-line", "/admin/statistics"),
    LAPTOPS("Quản lý Laptop", "bi-pc-display", "/admin/laptops"),
    CATEGORIES("Danh mục Laptop", "bi-tags", "/admin/categories"),
    BRANDS("Thương hiệu Laptop", "bi-award", "/admin/brands"),
    IMPORT_RECEIPTS("Phiếu nhập kho", "bi-truck-flatbed", "/admin/import-receipts"),
    ORDERS("Quản lý Đơn hàng", "bi-box-seam", "/admin/orders"),
    CUSTOMERS("Quản lý Khách hàng", "bi-people", "/admin/customers"),
    WARRANTIES("Bảo hành & Serial", "bi-shield-check", "/admin/warranties"),
    REVIEWS("Duyệt Đánh giá", "bi-star-half", "/admin/reviews"),
    SUPPORT_CHAT("Hỗ trợ trực tuyến", "bi-chat-dots", "/admin/content");

    private final String displayName;
    private final String iconClass;
    private final String baseUrl;

    AdminModule(String displayName, String iconClass, String baseUrl) {
        this.displayName = displayName;
        this.iconClass = iconClass;
        this.baseUrl = baseUrl;
    }

    public String getDisplayName() { return displayName; }
    public String getIconClass() { return iconClass; }
    public String getBaseUrl() { return baseUrl; }
}
