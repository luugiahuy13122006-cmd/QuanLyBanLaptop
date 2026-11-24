package com.laptopshop.util;

/**
 * Chuẩn hóa tham số phân trang để tránh lỗi page âm hoặc size quá lớn gây DoS bộ nhớ.
 */
public final class PaginationUtils {

    private PaginationUtils() {}

    public static int clampPage(int page) {
        return Math.max(0, page);
    }

    public static int clampSize(int size, int defaultSize, int maxSize) {
        if (size <= 0) return defaultSize;
        return Math.min(size, maxSize);
    }
}
