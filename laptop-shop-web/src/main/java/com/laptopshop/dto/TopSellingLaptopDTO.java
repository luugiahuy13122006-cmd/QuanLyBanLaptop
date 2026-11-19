package com.laptopshop.dto;

// Thành viên phụ trách: TV4
// DTO thống kê top laptop bán chạy nhất (khớp truy vấn Top 10 laptop bán chạy nhất trong QL_LAPTOP.sql).

import java.math.BigDecimal;

public class TopSellingLaptopDTO {

    private Integer laptopId;
    private String sku;
    private String laptopName;
    private String brand;
    private Long totalQuantity;
    private BigDecimal totalAmount;

    public TopSellingLaptopDTO() {
    }

    public TopSellingLaptopDTO(Integer laptopId, String sku, String laptopName, String brand, Long totalQuantity, BigDecimal totalAmount) {
        this.laptopId = laptopId;
        this.sku = sku;
        this.laptopName = laptopName;
        this.brand = brand;
        this.totalQuantity = totalQuantity != null ? totalQuantity : 0L;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public Integer getLaptopId() {
        return laptopId;
    }

    public void setLaptopId(Integer laptopId) {
        this.laptopId = laptopId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getLaptopName() {
        return laptopName;
    }

    public void setLaptopName(String laptopName) {
        this.laptopName = laptopName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public Long getTotalQuantitySold() {
        return totalQuantity;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getTotalRevenue() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
