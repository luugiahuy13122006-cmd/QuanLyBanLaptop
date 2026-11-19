package com.laptopshop.dto;

// Thành viên phụ trách: TV4
// DTO chứa dữ liệu doanh thu thống kê theo từng tháng của năm (khớp truy vấn Thống kê doanh thu theo tháng trong QL_LAPTOP.sql).

import java.math.BigDecimal;

public class MonthlyRevenueDTO {

    private Integer month;
    private Long orderCount;
    private BigDecimal revenue;

    public MonthlyRevenueDTO() {
    }

    public MonthlyRevenueDTO(Integer month, BigDecimal revenue) {
        this.month = month;
        this.orderCount = 0L;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }

    public MonthlyRevenueDTO(Integer month, Long orderCount, BigDecimal revenue) {
        this.month = month;
        this.orderCount = orderCount != null ? orderCount : 0L;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public Long getTotalOrders() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
