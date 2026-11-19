package com.laptopshop.dto;

// Thành viên phụ trách: TV4
// DTO đại diện cho một sản phẩm laptop và số lượng cần mua trong đơn hàng.

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OrderItemRequest {

    @NotNull(message = "Mã LaptopID không được để trống")
    private Integer laptopId;

    @NotNull(message = "Số lượng mua không được để trống")
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    private Integer quantity;

    public OrderItemRequest() {
    }

    public OrderItemRequest(Integer laptopId, Integer quantity) {
        this.laptopId = laptopId;
        this.quantity = quantity;
    }

    public Integer getLaptopId() {
        return laptopId;
    }

    public void setLaptopId(Integer laptopId) {
        this.laptopId = laptopId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
